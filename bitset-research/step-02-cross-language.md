# Шаг 2. Кросс-языковой обзор API BitSet

## Резюме

Проанализированы BitSet-реализации в 5 языках/платформах (C++, Rust, C#, Scala, Swift) в дополнение к Java из шага 1. Ключевой вывод: современные реализации тяготеют к модели **«множество целых чисел»** (Scala `Set[Int]`, Swift `SetAlgebra`, Rust `fixedbitset`) вместо низкоуровневого «вектора бит». Scala и Swift предоставляют наиболее зрелую интеграцию с коллекционными интерфейсами. Scala — единственный язык с полноценным immutable/mutable split через отдельные классы с общим трейтом. Swift предлагает value-type семантику с copy-on-write и отдельный тип `BitArray` для use case «вектор бит». C# `BitArray` — пример устаревшего дизайна с множеством антипаттернов (нет value equality, нет навигации по битам, итерация по bool-значениям вместо индексов). Rust `bitvec` vs `fixedbitset` явно демонстрирует фундаментальный выбор между абстракциями «последовательность бит» и «множество индексов».

**Детальные артефакты по каждому языку:**
- C++: [`step-02-cpp-bitset.md`](step-02-cpp-bitset.md)
- Rust: [`step-02-rust-bitvec-fixedbitset.md`](step-02-rust-bitvec-fixedbitset.md)
- C#: [`step-02-csharp-bitarray.md`](step-02-csharp-bitarray.md)
- Scala: [`step-02-scala-bitset.md`](step-02-scala-bitset.md)
- Swift: [`step-02-swift-bitset.md`](step-02-swift-bitset.md)

---

## 1. Java (baseline — из шага 1)

Детальный API surface зафиксирован в [`step-01-kotlin-implementations.md`](step-01-kotlin-implementations.md), секция 1.

**Ключевые характеристики для сравнения:**
- **Тип:** `public class BitSet implements Cloneable, Serializable`
- **Мутабельность:** только мутабельный
- **Размер:** динамический (auto-grow)
- **Внутреннее представление:** `long[]` (64 бита на слово)
- **Интерфейсы коллекций:** не реализует `Collection`, `Set`, `Iterable`
- **Итерация:** `stream()` (Java 8+, возвращает `IntStream` индексов set-битов), `nextSetBit`/`nextClearBit`/`previousSetBit`/`previousClearBit`
- **Операторы:** нет (Java не поддерживает operator overloading)
- **Сериализация:** `Serializable`, `toByteArray()`/`toLongArray()`/`valueOf()`
- **Три семантики размера:** `size()` (ёмкость), `length()` (idx MSB+1), `cardinality()` (popcount)
- **Equals/hashCode:** value-based (trailing zeros игнорируются)
- **Thread safety:** не thread-safe

---

## 2. C++

Детальный анализ: [`step-02-cpp-bitset.md`](step-02-cpp-bitset.md).

### 2.1 `std::bitset<N>` (стандартная библиотека)

| Аспект | Значение |
|---|---|
| Тип | `template<size_t N> class bitset` |
| Мутабельность | мутабельный (с const-корректностью) |
| Размер | compile-time фиксированный (`N`), не изменяем |
| Внутреннее представление | implementation-defined (обычно `unsigned long[]`) |
| Интерфейсы | не реализует Container/Iterable (осознанно) |
| Итерация | **нет** — только ручной цикл `for (i = 0; i < N; ++i) if (bs.test(i))` |
| Операторы | полный набор: `[]`, `&=`, `\|=`, `^=`, `~`, `<<=`, `>>=`, `<<`, `>>`, `==`, `!=` |
| Конвертации | `to_string()`, `to_ulong()`, `to_ullong()` |
| Хэширование | `std::hash<bitset<N>>` (C++11) |

**Ключевые решения:** compile-time размер обеспечивает stack allocation и constexpr-поддержку (C++23), но исключает динамические use cases. Нет итераторов, нет навигации по set-битам, нет range-операций.

### 2.2 `boost::dynamic_bitset<Block, Allocator>`

| Аспект | Значение |
|---|---|
| Тип | `template<typename Block = unsigned long, typename Allocator = ...> class dynamic_bitset` |
| Мутабельность | мутабельный (с const-корректностью) |
| Размер | runtime, явный resize (`resize`, `push_back`, `append`), **нет auto-grow** |
| Интерфейсы | не реализует Container (из-за proxy reference, как `vector<bool>`) |
| Итерация | `find_first()` / `find_next(pos)` → `npos` sentinel |
| Операторы | `std::bitset` + `operator-` (set difference), `<`/`<=`/`>`/`>=` (лексикографический) |
| Конвертации | `to_string()`, `to_ulong()`, `to_block_range()` / `from_block_range()` |
| Set-теоретические | `is_subset_of()`, `is_proper_subset_of()`, `intersects()` |
| Доп. операции | `test_set()` (get + set), range-версии `set`/`reset`/`flip`, `reserve`/`capacity`/`shrink_to_fit` |

**Ключевые решения:** расширяет `std::bitset` динамическим размером, block-level interop, set-теоретическими предикатами и навигацией. Но не решает проблему отсутствия итераторов. `operator-` для set difference — удобная идея. `npos` sentinel вместо `-1` — альтернатива Java-подходу.

---

## 3. Rust

Детальный анализ: [`step-02-rust-bitvec-fixedbitset.md`](step-02-rust-bitvec-fixedbitset.md).

### 3.1 `bitvec` (v1.0.1) — «последовательность бит»

| Аспект | Значение |
|---|---|
| Типы | `BitVec<T, O>` (dynamic), `BitSlice<T, O>` (borrowed), `BitArray<A, O>` (stack-fixed), `BitBox<T, O>` (heap-fixed) |
| Мутабельность | Rust ownership: owned мутация, `&mut` borrow, `&` read-only |
| Размер | `BitVec` — динамический (Vec-подобный API); `BitArray` — compile-time fixed |
| Интерфейсы | `IntoIterator`, `FromIterator<bool>`, `Index`, `BitAnd/Or/Xor/Not`, `Eq`, `Ord`, `Hash`, `Serialize/Deserialize` |
| Итерация | `iter()` (все биты как bool), `iter_ones()` / `iter_zeros()` (индексы set/clear бит) |
| Операторы | полный набор: `&`, `\|`, `^`, `!`, `<<=`, `>>=`, `[]` (с range sub-slicing) |
| Конвертации | `from_vec()` / `into_vec()` (zero-copy), `from_slice()`, serde |
| Queries | `count_ones()`, `count_zeros()`, `first_one()`, `first_zero()`, `last_one()`, `last_zero()`, `leading_*/trailing_*` |

**Ключевой паттерн:** Deref-цепочка `BitVec → BitSlice` (аналог `Vec<T> → [T]`). Все read-only и slice-операции определены на `BitSlice`, а owned-типы получают их через Deref. Generic-параметры `T: BitStore` и `O: BitOrder` позволяют контролировать тип хранения и порядок бит.

### 3.2 `fixedbitset` (v0.5.7) — «множество целых чисел»

| Аспект | Значение |
|---|---|
| Тип | `pub struct FixedBitSet` (без generics) |
| Мутабельность | мутабельный (owned) |
| Размер | фиксированный с `grow()` (только рост) |
| Интерфейсы | `IntoIterator` (по set-битам!), `FromIterator<usize>`, `Index`, `BitAnd/Or/Xor`, `Eq`, `Ord`, `Hash`, `Serialize/Deserialize` |
| Итерация | `ones()` (set-биты), `zeroes()` (clear-биты), `into_ones()` (consuming) |
| Операторы | `&`, `\|`, `^`, `&=`, `\|=`, `^=`, `[]`; **нет `!` (Not)** |
| Set-операции | `union_with`, `intersect_with`, `difference_with`, `symmetric_difference_with`; `is_disjoint`, `is_subset`, `is_superset` |
| Уникальные | `*_count()` (popcount результата без аллокации), `minimum()` / `maximum()`, SIMD-оптимизация (SSE2/AVX/Wasm SIMD) |

**Ключевой контраст с bitvec:** `fixedbitset` — модель множества (`FromIterator<usize>`, `IntoIterator` по set-битам, `insert`/`remove`/`contains` терминология). `bitvec` — модель последовательности (`FromIterator<bool>`, `IntoIterator` по всем битам). Осознанное отсутствие `Not` в fixedbitset — для множества неопределённого размера инверсия семантически неоднозначна.

---

## 4. C#

Детальный анализ: [`step-02-csharp-bitarray.md`](step-02-csharp-bitarray.md).

### `System.Collections.BitArray`

| Аспект | Значение |
|---|---|
| Тип | `public sealed class BitArray : ICloneable, ICollection` |
| Мутабельность | только мутабельный |
| Размер | фиксированный с явным resize через settable `Length` |
| Внутреннее представление | `int[]` (32 бита на слово) |
| Интерфейсы | `ICollection`, `IEnumerable` (non-generic), `ICloneable` |
| Итерация | `GetEnumerator()` → **bool-значения** (не индексы set-битов!) |
| Операторы | indexer `this[int]` только; **нет** `&`, `\|`, `^`, `~` |
| Bulk-операции | `And`, `Or`, `Xor`, `Not` — in-place, возвращают `this` (chaining); **требуют одинаковых длин** |
| Отсутствует | `AndNot`, `Intersects`, навигация по битам, range operations, value equality |
| Конвертации | конструкторы из `byte[]`, `bool[]`, `int[]`; `CopyTo(Array, int)` |
| Shift | `LeftShift(n)`, `RightShift(n)` (с .NET Core 2.0) |
| Cardinality | `PopCount()` — **только с .NET 11** (2026); `HasAllSet()`/`HasAnySet()` с .NET 8 |
| Equals/Hash | **не переопределены** (reference equality, identity hash) |
| ToString | возвращает `"System.Collections.BitArray"` |

**Антипаттерны:** C# `BitArray` — пример устаревшего дизайна, который за 25 лет добавил лишь 5 новых методов. Ключевые проблемы: нет value equality, итерация по bool (не по индексам set-битов), strict size matching для bulk ops, нет навигации, non-generic `IEnumerable` с boxing overhead. Единственные полезные идеи: indexer, chaining (return `this`), shift-операции, конструктор с default value.

### `System.Collections.Specialized.BitVector32`

Отдельная `struct` (value type) для ровно 32 бит. Stack-allocated, zero-allocation, двухрежимная работа (bit flags по маскам / packed integers по секциям). Не является альтернативой `BitArray` для общих задач — специализация для performance-critical кода с известным ≤32 бит.

---

## 5. Scala

Детальный анализ: [`step-02-scala-bitset.md`](step-02-scala-bitset.md).

### `scala.collection.immutable.BitSet` и `scala.collection.mutable.BitSet`

| Аспект | `immutable.BitSet` | `mutable.BitSet` |
|---|---|---|
| Тип | `sealed abstract class` | `class` |
| Супертипы | `AbstractSet[Int]`, `SortedSet[Int]`, `Serializable` | `AbstractSet[Int]`, `SortedSet[Int]`, `Serializable` |
| Хранение | `Long` fields (`BitSet1`, `BitSet2`) или `Array[Long]` (`BitSetN`) | `var elems: Array[Long]` |
| Мутация | возвращает новый BitSet (`+`, `-`, `\|`, `&`, `^`, `&~`) | модифицирует in-place (`+=`, `-=`, `\|=`, `&=`, `^=`, `&~=`) |
| Размер | динамический, автоматическое уменьшение до `BitSet1`/`BitSet2` | динамический, удваивает массив; `clear()` обнуляет без уменьшения |
| Интерфейсы | **полный `Set[Int]` + `SortedSet[Int]` + `Function1[Int, Boolean]`** | аналогично + `Growable`, `Shrinkable` |
| Итерация | `iterator`, `foreach`, for-comprehensions, `stepper` (Java stream interop) | аналогично |
| Операторы | `+`, `-`, `--`, `++`, `&`, `\|`, `^`, `&~` | `+=`, `-=`, `++=`, `--=`, `&=`, `\|=`, `^=`, `&~=` + все из immutable |
| Конвертации | `toBitMask: Array[Long]`, `fromBitMask(Array[Long])`, все стандартные (`toList`, `toSet`, `toArray`, ...) | + `toImmutable`, `clone()` |
| Map/flatMap | `map(Int => Int)` возвращает `BitSet`; `map(Int => B)` → `SortedSet[B]` | аналогично |

**Уникальные решения:**

1. **`Set[Int]` + `SortedSet[Int]`** — богатейшая интеграция с коллекциями из всех языков. BitSet = полноценное множество целых чисел, работающее со всей collections API: `map`, `flatMap`, `filter`, `foldLeft`, `zip`, for-comprehensions. `Set[A]` расширяет `A => Boolean`, поэтому BitSet используется как предикат: `list.filter(bitSet)`.

2. **Immutable/Mutable split через отдельные классы** — единственный язык с полноценным разделением. Общий трейт `collection.BitSet` с `BitSetOps` определяет shared-операции. Immutable возвращает новые объекты, mutable модифицирует in-place.

3. **Small-set optimization** — `BitSet1` (1 Long, без массива) и `BitSet2` (2 Long, без массива) для immutable варианта. Автоматическое уменьшение при операциях (`BitSetN` → `BitSet1`/`BitSet2`).

4. **Нет `flip`/`negate`, нет `nextSetBit`** — осознанное решение. Scala не экспонирует низкоуровневые bit-операции; все делается через set-операции и итерацию.

---

## 6. Swift

Детальный анализ: [`step-02-swift-bitset.md`](step-02-swift-bitset.md).

### `BitSet` из swift-collections

| Аспект | Значение |
|---|---|
| Тип | `public struct BitSet` (value type) |
| Пакет | swift-collections (НЕ часть стандартной библиотеки) |
| Мутабельность | value type с CoW; mutating methods помечены `mutating` |
| Размер | динамический (auto-grow, auto-shrink) |
| Внутреннее представление | `[_Word]` (обёртка над `UInt`, platform word size) |
| Протоколы | `SetAlgebra`, `Sequence`, `Collection`, `BidirectionalCollection`, `Equatable`, `Hashable`, `Codable`, `ExpressibleByArrayLiteral`, `Sendable` |
| Элемент | `Int` (множество неотрицательных целых) |
| Set algebra | `union`, `intersection`, `symmetricDifference`, `subtracting`, `formUnion`, `formIntersection`, `formSymmetricDifference`, `subtract`; `isSubset`, `isSuperset`, `isStrictSubset`, `isStrictSuperset`, `isDisjoint`, `isEqualSet` — по 4 overloads каждый (BitSet, BitSet.Counted, Range, Sequence) = **56 методов** |
| Итерация | `Sequence`/`Collection` — итерация по member values (`Int`), в ascending order |
| Subscripts | `subscript(member: Int)` → `Bool` (get/set/toggle); `subscript(members: Range<Int>)` → `Slice` |
| Операторы | **нет** bitwise operators; только `==` |
| Конвертации | `init(words:)`, `init(bitPattern:)`, `init(_ range: Range<Int>)`, `Codable` (UInt64-based) |
| Counted | `BitSet.Counted` — обёртка с кешированным `count` для O(1) `count`/`isEmpty` |
| Thread safety | `Sendable` (value type, safe для concurrency) |
| NOT `RandomAccessCollection` | index advancement O(d) между sparse элементами |

**Уникальные решения:**

1. **`SetAlgebra`-first identity** — BitSet определяется через `SetAlgebra` протокол. Все операции — set-теоретические (`insert`/`remove`/`contains`, не `set`/`clear`/`get`).

2. **Отдельный `BitArray`** — swift-collections разделяет «множество целых» (`BitSet`: `SetAlgebra`, `Collection`) и «вектор бит» (`BitArray`: `MutableCollection`, `RandomAccessCollection`, `RangeReplaceableCollection`). Чёткое концептуальное разделение.

3. **`BitSet.Counted`** — optional wrapper с кешированным popcount. Избегает overhead для пользователей, которым не нужен O(1) `count`.

4. **`subscript(member:)`** с `.toggle()` — `bits[member: 5].toggle()` для инверсии; `bits[member: 5] = true` для установки.

5. **Overloads для Range и Sequence** — каждая set-операция принимает `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>`, обеспечивая гибкость без создания промежуточных объектов.

6. **Codable с UInt64** — сериализация в `UInt64`-слова (не platform-native `UInt`) для кроссплатформенной переносимости.

7. **Нет bitwise operators** — осознанное решение Swift: `SetAlgebra` не определяет `|`, `&`, `^`. Все операции через именованные методы.

---

## 7. Сравнительные таблицы по осям

### 7.1 Мутабельность

| Язык | Реализация | Мутабельный | Immutable вариант | Модель |
|---|---|---|---|---|
| Java | `BitSet` | Да | Нет | Один мутабельный класс |
| C++ | `std::bitset<N>` | Да | Нет (const-корректность) | Один класс с const overloads |
| C++ | `boost::dynamic_bitset` | Да | Нет (const-корректность) | Один класс с const overloads |
| Rust | `bitvec` (`BitVec`) | Да (`&mut`) | Read-only через `&BitSlice` | Ownership/borrowing модель |
| Rust | `fixedbitset` | Да (`&mut`) | Read-only через `&` | Ownership/borrowing модель |
| C# | `BitArray` | Да | Нет | Один мутабельный sealed class |
| Scala | `immutable.BitSet` | Нет | **Да** | **Отдельные классы с общим трейтом** |
| Scala | `mutable.BitSet` | Да | Нет | **Отдельные классы с общим трейтом** |
| Swift | `BitSet` | Да (`mutating`) | Value type с CoW | **Struct + copy-on-write** |

**Тенденции:**
- Большинство реализаций — только мутабельные.
- Scala — единственный язык с полноценным immutable/mutable split через раздельные классы, общую иерархию типов и различную семантику операций.
- Swift решает проблему через value-type семантику (struct + CoW): каждое присваивание создаёт логическую копию, mutating методы помечены компилятором.
- Rust решает через ownership/borrowing: `&BitSlice` (read-only view) vs `&mut BitSlice` (mutable view) vs `BitVec` (owned).
- Для Kotlin наиболее релевантна модель Scala (read-only interface + mutable implementation), совместимая с паттерном коллекций Kotlin (`List`/`MutableList`).

### 7.2 Модель размера

| Язык | Реализация | Тип размера | Auto-grow | Resize | Capacity management |
|---|---|---|---|---|---|
| Java | `BitSet` | Динамический | **Да** (при `set()`) | Нет явного | Нет (скрыта) |
| C++ | `std::bitset<N>` | Compile-time fixed | Нет | Нет | N/A |
| C++ | `boost::dynamic_bitset` | Runtime, явный | **Нет** (assert при OOB) | `resize`, `push_back`, `append` | `reserve`, `capacity`, `shrink_to_fit` |
| Rust | `bitvec` (`BitVec`) | Динамический | Да (через `push`) | `resize`, `truncate` | `reserve`, `capacity`, `shrink_to_fit` |
| Rust | `fixedbitset` | Фикс. + grow | Нет (panic при OOB) | `grow()` (только рост) | Нет |
| C# | `BitArray` | Фикс. + explicit resize | Нет (exception при OOB) | `Length` setter | Нет |
| Scala | оба варианта | Динамический | **Да** (при `+`/`+=`) | Автоматически | `ensureCapacity` (mutable) |
| Swift | `BitSet` | Динамический | **Да** (при `insert`) | Автоматически | `reserveCapacity` |

**Тенденции:**
- Auto-grow (Java, Scala, Swift) — наиболее удобная модель для пользователей.
- C++ boost и Rust fixedbitset требуют явного управления размером — более безопасно, но менее удобно.
- C# — гибрид: фиксированный, но с settable `Length` (awkward API).
- Для Kotlin: auto-grow наиболее ожидаем (совместимо с Java `BitSet` и паттерном коллекций Kotlin).

### 7.3 Интерфейсы коллекций

| Язык | Реализация | Set | SortedSet | Iterable / Collection | Function / Predicate |
|---|---|---|---|---|---|
| Java | `BitSet` | **Нет** | Нет | **Нет** (только `stream()`) | Нет |
| C++ | обе | Нет | Нет | **Нет** (proxy reference проблема) | Нет |
| Rust | `bitvec` | Нет | Нет | `IntoIterator` (bool-значения) | Нет |
| Rust | `fixedbitset` | Нет (но set-API) | Нет | `IntoIterator` (индексы set-бит) | Нет |
| C# | `BitArray` | Нет | Нет | `IEnumerable` (bool-значения, non-generic) | Нет |
| Scala | `BitSet` | **`Set[Int]`** | **`SortedSet[Int]`** | **Полная collections API** | **`Int => Boolean`** |
| Swift | `BitSet` | **`SetAlgebra`** | Нет (но sorted by nature) | **`BidirectionalCollection`** | Нет |

**Тенденции:**
- Java, C++ и C# **не** интегрируют BitSet в свои коллекционные фреймворки — это главный источник жалоб и boilerplate.
- Scala — максимальная интеграция: `Set[Int]` + `SortedSet[Int]`, полная collections API, BitSet как `Function1[Int, Boolean]`.
- Swift — сильная интеграция через `SetAlgebra` + `Collection`.
- Rust — средняя: trait implementations (`IntoIterator`, `FromIterator`) обеспечивают совместимость с экосистемой, но нет формального `Set` trait.
- Для Kotlin: реализация `Set<Int>` или хотя бы `Iterable<Int>` — ключевое ожидание пользователей. Scala-подход наиболее близок к паттерну коллекций Kotlin.

### 7.4 Итерация

| Язык | Реализация | По set-битам (индексы) | По всем битам (bool) | По clear-битам | Обратная итерация | Навигация |
|---|---|---|---|---|---|---|
| Java | `BitSet` | `stream()` (IntStream) | Нет | Нет | Нет (stream) | `nextSetBit`, `nextClearBit`, `previousSetBit`, `previousClearBit` |
| C++ | `std::bitset` | **Нет** | Ручной цикл | Ручной цикл | Нет | **Нет** |
| C++ | `boost` | `find_first`/`find_next` | Ручной цикл | Нет | **Нет** (только forward) | `find_first`, `find_next` |
| Rust | `bitvec` | `iter_ones()` | `iter()` | `iter_zeros()` | `DoubleEndedIterator` | `first_one`, `first_zero`, `last_one`, `last_zero` |
| Rust | `fixedbitset` | `ones()`, `into_ones()` | Нет | `zeroes()` | `DoubleEndedIterator` (для `ones`) | `minimum()`, `maximum()` |
| C# | `BitArray` | **Нет** | `GetEnumerator()` (bool) | Нет | Нет | **Нет** |
| Scala | `BitSet` | `iterator`, `foreach`, for-comprehensions | Нет (только set-биты) | Нет | `iteratorFrom(start)` | `min`, `max`, `iteratorFrom`, `range`/`from`/`until` |
| Swift | `BitSet` | `makeIterator()`, for-in | Нет (только set-биты) | Нет | **Да** (`BidirectionalCollection`) | `first`, `last`, `index(after:)`, `index(before:)` |

**Тенденции:**
- Итерация **по индексам set-битов** — доминирующий паттерн (Java, Rust, Scala, Swift). C# — антипаттерн (итерирует bool-значения).
- Итерация по clear-битам — редка (bitvec, fixedbitset), но полезна для некоторых алгоритмов.
- Обратная итерация / bidirectional — поддерживается в Swift, Rust (DoubleEndedIterator), частично в Java (`previousSetBit`).
- `minimum()`/`maximum()` (fixedbitset) — более чистый API, чем Java `nextSetBit(0)` / цикл.
- Scala `iteratorFrom(start)` — мощный примитив для SortedSet.
- Для Kotlin: `Iterable<Int>` / `Iterator<Int>` по set-битам — ожидаемый минимум. `forEachBit {}` (inline) — для производительности.

### 7.5 Операторы и синтаксический сахар

| Язык | `[]` get | `[]` set | `&`/`\|`/`^` | `~`/`!` (NOT) | Set diff op | Shift `<<`/`>>` | `in` / contains op | Literal syntax |
|---|---|---|---|---|---|---|---|---|
| Java | `get(i)` | `set(i)` | Нет | Нет | Нет | Нет | Нет | Нет |
| C++ | `[]` / `test` | `[]` = proxy | `&=` `\|=` `^=` + free | `~` | `-` (boost) | `<<=` `>>=` + free | Нет | Нет |
| Rust (bitvec) | `[]` → proxy | `set()` | `&` `\|` `^` | `!` | Нет | `<<` `>>` | Нет | `bitvec![...]` macro |
| Rust (fixedbitset) | `[]` → bool | `set()`, `insert()` | `&` `\|` `^` | **Нет** | Нет | Нет | `contains()` | `collect()` |
| C# | `this[i]` | `this[i]=` | Нет | Нет | Нет | Методы | Нет | Нет |
| Scala | `apply(i)` → Bool | `update(i, Bool)` | `&` `\|` `^` | Нет | `&~` | Нет | `contains`, `apply` | `BitSet(1, 3, 5)` |
| Swift | `[member: i]` | `[member: i]=` | Нет | Нет | Нет | Нет | `contains(i)` | `[1, 3, 5]` literal |

**Тенденции:**
- `[]` для чтения — универсально (кроме Java из-за языковых ограничений).
- `[]` для записи — C++ (proxy), C# (indexer), Swift (`subscript(member:)`), Scala (`update`).
- Bitwise operators — C++ и Rust имеют полный набор; Scala использует set-теоретические операторы (`&`, `|`, `^`, `&~`); Swift и Java — без операторов.
- NOT (`~`/`!`) — проблематичен для dynamic-sized sets (семантика инверсии неопределена). bitvec реализует, fixedbitset — нет.
- Set difference как оператор: C++ boost (`-`), Scala (`&~`) — удобно.
- Для Kotlin: `operator get`, `operator set`, infix `and`/`or`/`xor` — наиболее идиоматично. `operator contains` (`in`) — ожидаем.

### 7.6 Сериализация и interop

| Язык | В raw массив | Из raw массива | Из коллекций | В коллекции | В строку | Стандартная сериализация | Кросс-платформенная |
|---|---|---|---|---|---|---|---|
| Java | `toLongArray()`, `toByteArray()` | `valueOf(long[])`, `valueOf(byte[])` | Нет | Нет (только `stream()`) | `{0, 2, 5}` | `Serializable` | Нет |
| C++ std | `to_ulong()`, `to_ullong()` | Конструктор из `ulong` | Нет | Нет | `to_string()` `"01010"` | Нет | Нет |
| C++ boost | `to_block_range()` | `from_block_range()`, конструктор | Нет | Нет | `to_string()` `"01010"` | Нет | Нет |
| Rust (bitvec) | `as_raw_slice()`, `into_vec()` | `from_vec()`, `from_slice()` | `FromIterator<bool>` | `IntoIterator` | Binary `[01101]` | Serde (optional) | Через serde |
| Rust (fixedbitset) | `as_slice()` | `with_capacity_and_blocks()` | `FromIterator<usize>` | `IntoIterator` | `{2, 5, 7}` | Serde (optional) | Через serde |
| C# | `CopyTo(int[])`, `CopyTo(byte[])` | Конструктор `int[]`, `byte[]`, `bool[]` | Нет | `CopyTo(bool[])` | `"System.Collections.BitArray"` | Нет (.NET Core+) | Нет |
| Scala | `toBitMask: Array[Long]` | `fromBitMask(Array[Long])` | Конструктор из `Int*` | `toList`, `toSet`, `toArray`, ... | `"BitSet(1, 3, 5)"` | `Serializable` | Java Serialization |
| Swift | Нет прямого | `init(words:)`, `init(bitPattern:)` | `init(_ sequence:)` | `Array(bitSet)` через Collection | `"[1, 2, 3]"` | `Codable` (UInt64) | **Да** (UInt64-based) |

**Тенденции:**
- `toLongArray()`/`fromLongArray()` — стандарт для raw interop (Java, Scala, C++ boost).
- Swift `Codable` с UInt64-словами — лучший подход к кросс-платформенной сериализации.
- Конвертации в/из коллекций (`toList`, `toSet`, `FromIterator`) — ключевой пробел Java, хорошо покрыт в Scala и Rust.
- `toString()` формат: «множество» (`{0, 2, 5}`) vs «бинарный» (`"01010"`) — разные языки делают разный выбор. Формат «множество» более распространён (Java, fixedbitset, Scala, Swift).
- Для Kotlin: `toLongArray()`/`fromLongArray()` + `toSet()`/`toBitSet()` extension — минимум. `kotlinx.serialization` support — отдельный вопрос (шаг 8c).

---

## 8. Сводка дизайн-решений по языкам

### Ключевые архитектурные решения

| Решение | Java | C++ std | C++ boost | Rust bitvec | Rust fixedbitset | C# | Scala | Swift |
|---|---|---|---|---|---|---|---|---|
| **Концепт. модель** | Bit vector (с set-like ops) | Bit vector | Bit vector | **Sequence of bits** | **Set of integers** | Bit array | **Set of integers** | **Set of integers** |
| **Мутабельность** | Mutable | Mutable | Mutable | Ownership | Mutable | Mutable | **Immutable + Mutable** | Value type (CoW) |
| **Размер** | Dynamic auto | Fixed compile | Dynamic explicit | Dynamic | Fixed + grow | Fixed + resize | Dynamic auto | Dynamic auto |
| **Коллекции** | Нет | Нет | Нет | Traits | Traits | Non-generic | **Full Set[Int]** | **SetAlgebra + Collection** |
| **Итерация set** | stream() | — | find_first/next | iter_ones() | ones() | — | iterator/foreach | Collection iteration |
| **Cardinality** | `cardinality()` | `count()` | `count()` | `count_ones()` | `count_ones(..)` | `PopCount()` (.NET 11) | `size` | `count` (O(max)) |
| **Value equality** | Да | Да | Да | Да | Да | **Нет** | Да | Да |
| **Internal repr** | `long[]` | impl-defined | `Block[]` | Generic `T` | `usize[]` | `int[]` | `Array[Long]` | `[UInt]` |

### Уникальные идеи каждого языка

| Язык | Идея | Релевантность для Kotlin |
|---|---|---|
| Scala | `Set[Int]` + `SortedSet[Int]`, immutable/mutable split, `BitSet1`/`BitSet2` small-set optimization, `apply(i)` = `contains`, type-preserving `map` | **Высокая** — паттерн коллекций Kotlin аналогичен Scala |
| Swift | `SetAlgebra`, `BitSet.Counted`, `subscript(member:)` с toggle, отдельный `BitArray`, `Codable` с UInt64, `ExpressibleByArrayLiteral`, overloads для Range/Sequence | **Высокая** — современный дизайн, value-type семантика |
| Rust (fixedbitset) | `*_count()` (popcount без аллокации), `minimum()`/`maximum()`, SIMD, `grow_and_insert`, `contains_all_in_range`/`contains_any_in_range` | **Средняя** — полезные оптимизации |
| Rust (bitvec) | Deref delegation (`BitVec → BitSlice`), `BitOrder` parametrization, `iter_ones()`/`iter_zeros()`, chunks/windows/split | **Средняя** — delegation паттерн полезен; bit order — скорее нет |
| C++ boost | `find_first`/`find_next` + `npos` sentinel, `operator-` (set diff), `is_subset_of`/`is_proper_subset_of`, `test_set` (get+set), block-level interop | **Средняя** — хорошие API решения |
| C# | Indexer с chaining (return `this`), конструктор с default value, shift-операции, `BitVector32` для ≤32 бит | **Низкая** — в основном антипаттерны |

---

## 9. Ключевые выводы и тенденции

### 9.1 Фундаментальный выбор: «множество целых» vs «вектор бит»

Два подхода к абстракции:
- **«Множество целых чисел»** (Scala `Set[Int]`, Swift `SetAlgebra`, Rust `fixedbitset`): `contains(5)`, `insert(5)`, `remove(5)`, итерация по членам.
- **«Вектор бит»** (Rust `bitvec`, C++ `bitset`, C# `BitArray`): `get(5)`, `set(5, true)`, итерация по индексам с bool-значениями.

Java `BitSet` — гибрид: API в стиле «вектор бит» (`get`/`set`/`clear`), но `stream()` возвращает индексы set-битов (set-семантика). Современные реализации тяготеют к модели «множество». Swift явно разделяет эти модели на `BitSet` и `BitArray`.

**Рекомендация для Kotlin:** модель «множество целых» лучше интегрируется с Kotlin collections (`Set<Int>`, `Iterable<Int>`), но bit-level API (`set`, `clear`, `flip`, `get`) необходим для совместимости с Java и low-level use cases. Оптимальный подход: set-like API первичен (интерфейсы, итерация), bit-level API — как дополнение.

### 9.2 Интеграция с коллекциями — ожидание пользователей

Спектр интеграции (от минимальной к максимальной):
1. **Нет интеграции** — Java, C++, C# (исторически)
2. **Trait/protocol conformance** — Rust (`IntoIterator`, `FromIterator`)
3. **Collection protocol** — Swift (`SetAlgebra`, `Collection`)
4. **Полная коллекционная иерархия** — Scala (`Set[Int]`, `SortedSet[Int]`, `Iterable`, `map`/`flatMap`/`filter`)

Тенденция: современные дизайны (Swift 2022, Scala 2.13) стремятся к максимальной интеграции. Пользователи ожидают `for (bit in bitSet)`, `bitSet.filter { ... }`, `5 in bitSet`.

### 9.3 Mutable/immutable split

Три подхода:
1. **Только мутабельный** — Java, C++, C#. Самый простой, но ограничивает safety и functional-style код.
2. **Отдельные классы** — Scala. Максимальная гибкость, но сложнее API.
3. **Value type с CoW** — Swift. Естественная иммутабельность через семантику значений.

Для Kotlin наиболее естественен подход, совместимый с паттерном `List`/`MutableList`: read-only interface (`BitSet`) + mutable implementation (`MutableBitSet`).

### 9.4 NOT/Complement проблема

Операция «побитовое дополнение» семантически неоднозначна для динамических множеств:
- `std::bitset<N>` и `bitvec`: `~`/`!` определены, т.к. размер фиксирован.
- `fixedbitset`: **нет** `Not` — осознанное решение (инвертировать до какой границы?).
- Java: нет общего `not()`, но есть `flip(from, to)` для диапазонов.
- Scala: нет `~`/`not`.
- Swift: нет complement.

Для Kotlin: `flip(index)`, `flip(range)` — безопасны. Глобальный `not()` / `operator inv()` — проблематичен без фиксированного размера.

### 9.5 Именование: cardinality/count/size/popcount

| Язык | «Кол-во set-битов» | «Общий размер» | «Ёмкость» |
|---|---|---|---|
| Java | `cardinality()` | `length()` (MSB+1), `size()` (capacity) | `size()` |
| C++ | `count()` | `size()` | — / `capacity()` (boost) |
| Rust | `count_ones()` | `len()` | — / `capacity()` |
| C# | `PopCount()` (.NET 11) | `Length` / `Count` | — |
| Scala | `size` (от `Set`) | `size` | `nwords` (internal) |
| Swift | `count` (от `Collection`) | — | `reserveCapacity` |

`cardinality` — Java-specific термин. Если BitSet реализует `Set<Int>`, `size` = число set-битов (как в Scala, Swift). `count` — наиболее распространённое имя для popcount (C++, Rust, Swift).

### 9.6 Итерация: set-биты как первичный паттерн

Все современные реализации (Scala, Swift, Rust fixedbitset) итерируют **по set-битам** (возвращая индексы `Int`), а не по всем битам. C# `BitArray` — антипаттерн (итерирует bool-значения). Java `stream()` — правильный подход, но ограничен Java 8+ API.

Для Kotlin: `Iterable<Int>` / `Iterator<Int>` по индексам set-битов — основной паттерн. Дополнительно: `forEachBit {}` (inline, высокопроизводительный), `asSequence()`.

---

## 10. Открытые вопросы для последующих шагов

1. **`Set<Int>` vs отдельная иерархия** — реализация `Set<Int>` обеспечивает максимальную интеграцию (Scala), но привносит ограничения (`equals` совместимость с `Set.equals`, boxing при итерации?). Swift выбрал `SetAlgebra` (отдельный протокол), не `Set`. Решение → шаг 8.

2. **Naming: `BitSet` vs `BitArray` vs другое** — Swift разделяет `BitSet` (множество) и `BitArray` (вектор). Для Kotlin имя `BitSet` привычно из Java. Если тип моделирует множество — `BitSet` уместно. Если вектор — `BitArray` точнее. Решение → шаг 8.

3. **`IntArray` vs `LongArray` на JS** — BitVector (из шага 1) использует `IntArray` из-за Long-эмуляции на JS. Swift использует platform-native `UInt`. Решение → шаг 10.

4. **Small-set optimization** — Scala `BitSet1`/`BitSet2` (immutable, без массива для ≤128 бит). Swift `BitSet.Counted` (отдельная обёртка). Для Kotlin: inline value class для single-word? Решение → шаг 8/10.

5. **`*_count()` операции** (fixedbitset) — `union_count()`, `intersection_count()` без аллокации. Полезно для graph algorithms. Стоит ли включать в первую версию API? → шаг 8.

6. **Отдельный `BitArray` тип?** — Swift явно разделяет use cases. Для Kotlin: один тип `BitSet` или два (`BitSet` + `BitArray`)? → шаг 8.
