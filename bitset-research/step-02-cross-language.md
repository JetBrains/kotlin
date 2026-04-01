# Шаг 2. Кросс-языковой обзор API BitSet

## Резюме

Проанализированы BitSet-реализации в 5 языках/платформах (C++, Rust, C#, Scala, Swift) в дополнение к Java из шага 1. Ключевой вывод: современные реализации тяготеют к модели **«множество целых чисел»** (Scala `Set[Int]`, Swift `SetAlgebra`, Rust `fixedbitset`) вместо низкоуровневого «вектора бит». Scala и Swift предоставляют наиболее зрелую интеграцию с коллекционными интерфейсами. Scala — единственный язык с полноценным immutable/mutable split через отдельные классы с общим трейтом. Swift предлагает value-type семантику с copy-on-write и отдельный тип `BitArray` для use case «вектор бит». C# `BitArray` — пример устаревшего дизайна с множеством антипаттернов (нет value equality, нет навигации по битам, итерация по bool-значениям вместо индексов). Rust `bitvec` vs `fixedbitset` явно демонстрирует фундаментальный выбор между абстракциями «последовательность бит» и «множество индексов».

**Входные данные:** [`step-01-kotlin-implementations.md`](step-01-kotlin-implementations.md) (ревизия существующих реализаций в экосистеме Kotlin, включая `java.util.BitSet` как baseline).

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

**Ключевые решения:** compile-time размер обеспечивает фиксированный inline storage внутри объекта (без heap-аллокации для хранения бит) и естественную constexpr-поддержку (частичную с C++11, полную с C++23), но исключает динамические use cases. Начиная с Boost 1.90.0 `dynamic_bitset` тоже поддерживает `constexpr` при компиляции в режиме C++20+, однако `std::bitset` остаётся compile-time типом с более сильной constexpr-историей. Нет итераторов, нет навигации по set-битам, нет range-операций.

### 2.2 `boost::dynamic_bitset<Block, AllocatorOrContainer>`

| Аспект | Значение |
|---|---|
| Тип | `template<typename Block = unsigned long, typename AllocatorOrContainer = ...> class dynamic_bitset` |
| Мутабельность | мутабельный (с const-корректностью) |
| Размер | runtime, явный resize (`resize`, `push_back`, `pop_back`, `push_front`, `pop_front` (Boost 1.90.0+), `append`), **нет auto-grow** |
| Интерфейсы | формально не моделирует Container; итераторы `begin`/`end`/`rbegin`/`rend` добавлены в Boost 1.90.0 (C++20 ranges; не LegacyForwardIterator) |
| Итерация | По set-битам: `find_first(pos = 0)` / `find_next(pos)` → `npos` sentinel; по clear-битам: `find_first_off(pos = 0)` / `find_next_off(pos)` (Boost 1.90.0+); по всем битам: итераторы `begin`/`end`/`rbegin`/`rend` (Boost 1.90.0+, обходят все позиции, возвращая bool/proxy) |
| Операторы | `std::bitset` + `operator-` (set difference), `<`/`<=`/`>`/`>=` (лексикографический); **бинарные операторы требуют `size() == rhs.size()`** |
| Конвертации | non-member `to_string(b, s)`, `to_ulong()`, `to_block_range()` / `from_block_range()` |
| Set-теоретические | `is_subset_of()`, `is_proper_subset_of()`, `intersects()` — **все требуют `size() == rhs.size()`** |
| Доп. операции | `test_set()` (get + set), `at()` (checked accessor; `std::out_of_range` при OOB), range-версии `set`/`reset`/`flip`, `reserve`/`capacity`/`shrink_to_fit` |

**Ключевые решения:** расширяет `std::bitset` динамическим размером, block-level interop, set-теоретическими предикатами и навигацией. **Важное ограничение:** все бинарные операторы (`&=`, `|=`, `^=`, `-=` и свободные `&`, `|`, `^`, `-`) и set-предикаты (`is_subset_of`, `is_proper_subset_of`, `intersects`) требуют `size() == rhs.size()` (assert при несовпадении) — это жёстче, чем Java `BitSet`, допускающий разные размеры. Boost 1.90.0 (декабрь 2025) добавил итераторы `begin`/`end`/`rbegin`/`rend` для обхода **всех битовых позиций** (не только set-битов; для set-bit навигации по-прежнему используются `find_first`/`find_next`), навигацию по clear-битам (`find_first_off`/`find_next_off`), lower-bound поиск (`find_first(pos)` с дефолтным `pos = 0`), и операции `push_front`/`pop_front` для управления LSB-концом. `operator-` для set difference — удобная идея. `npos` sentinel вместо `-1` — альтернатива Java-подходу.

> **Провенанс:** Утверждения о Boost 1.90.0+ (итераторы, `AllocatorOrContainer`, `find_first_off`/`find_next_off`, `push_front`/`pop_front`) воспроизводимы по актуальной official reference-документации Boost (обновлена к марту 2026). Подробнее — в [`step-02-cpp-bitset.md`](step-02-cpp-bitset.md), примечание о провенансе в §2.

---

## 3. Rust

Детальный анализ: [`step-02-rust-bitvec-fixedbitset.md`](step-02-rust-bitvec-fixedbitset.md).

### 3.1 `bitvec` (v1.0.1) — «последовательность бит»

| Аспект | Значение |
|---|---|
| Типы | `BitVec<T, O>` (dynamic), `BitSlice<T, O>` (borrowed), `BitArray<A, O>` (compile-time fixed, inline), `BitBox<T, O>` (heap-fixed) |
| Мутабельность | Rust ownership: owned мутация, `&mut` borrow, `&` read-only |
| Размер | `BitVec` — динамический (Vec-подобный API); `BitArray` — compile-time fixed |
| Интерфейсы | `IntoIterator`, `FromIterator<bool>` / `FromIterator<T>`, `Extend<bool>` / `Extend<T>`, `Index`, `BitAnd/Or/Xor/Not`, `Eq`, `Ord`, `Hash`, `Read`/`Write` (feature `std`), `Serialize/Deserialize` (feature `serde`), `Send`, `Sync` |
| Итерация | `iter()` (все биты; yields proxy refs `BitRef`, адаптеры `.by_refs()` → `&bool`, `.by_vals()` → `bool`), `iter_ones()` / `iter_zeros()` (индексы set/clear бит) |
| Операторы | полный набор: `&`, `\|`, `^`, `!`, `<<=`, `>>=`, `[]` (с range sub-slicing) |
| Конвертации | `from_vec()` / `into_vec()` (zero-copy), `from_slice()`, serde (feature `serde`) |
| Queries | `count_ones()`, `count_zeros()`, `first_one()`, `first_zero()`, `last_one()`, `last_zero()`, `leading_*/trailing_*` |

**Ключевой паттерн:** Deref-цепочка `BitVec → BitSlice` (аналог `Vec<T> → [T]`). Все read-only и slice-операции определены на `BitSlice`, а owned-типы получают их через Deref. Generic-параметры `T: BitStore` и `O: BitOrder` позволяют контролировать тип хранения и порядок бит.

### 3.2 `fixedbitset` (v0.5.7) — «set-like API поверх fixed-length bitmap»

| Аспект | Значение |
|---|---|
| Тип | `pub struct FixedBitSet` (без generics) |
| Мутабельность | мутабельный (owned) |
| Размер | фиксированный с `grow()` (только рост); in-place `union_with` / `symmetric_difference_with` неявно grow до `other.len()`; немутирующие операторы: `&` → `min(len)`, `\|`/`^` → `max(len)` |
| Интерфейсы | `FromIterator<usize>`, `Extend<usize>`, `Index`, `BitAnd/Or/Xor`, `Eq`, `Ord`, `Hash`, `Serialize/Deserialize` (feature `serde`); trait-level `IntoIterator` отсутствует |
| Итерация | `ones()` (set-биты), `zeroes()` (clear-биты), `into_ones()` (consuming) |
| Операторы | `&`, `\|`, `^`, `&=`, `\|=`, `^=`, `[]`; **нет `!` (Not)** |
| Set-операции | `union_with`, `intersect_with`, `difference_with`, `symmetric_difference_with`; `is_disjoint`, `is_subset`, `is_superset`; ленивые итераторы `intersection`, `union`, `difference`, `symmetric_difference` |
| Уникальные | Три слоя set-операций (in-place / counting / lazy iterators), `*_count()` (popcount результата без аллокации), `minimum()` / `maximum()`, SIMD-оптимизация (SSE2/AVX/Wasm SIMD) |

**Ключевой контраст с bitvec:** `fixedbitset` — set-like API (`FromIterator<usize>`, именованные итераторы `ones()` / `into_ones()` по set-битам, `insert`/`remove`/`contains` терминология), но **не** set-compatible семантика равенства: `Eq`/`Ord`/`Hash` учитывают длину и trailing zeros (`[0,1] != [0,1,0]`; `Ord` сравнивает сначала `length`, затем storage). Ещё одно проявление этой двойственности — семантика пустоты: `is_empty()` означает структурную пустоту (`len() == 0`), тогда как `is_clear()` — отсутствие установленных битов; bitset с `len() == 100` и всеми нулевыми битами: `is_empty() == false`, `is_clear() == true` (подробнее — §9.5). `bitvec` — модель последовательности (`FromIterator<bool>`, `IntoIterator` по всем битам). Осознанное отсутствие `Not` в fixedbitset — для множества неопределённого размера инверсия семантически неоднозначна.

---

## 4. C#

Детальный анализ: [`step-02-csharp-bitarray.md`](step-02-csharp-bitarray.md).

### `System.Collections.BitArray`

| Аспект | Значение |
|---|---|
| Тип | `public sealed class BitArray : ICloneable, ICollection` |
| Мутабельность | только мутабельный |
| Размер | фиксированный с явным resize через settable `Length` |
| Внутреннее представление | `byte[]` с .NET 10 (до .NET 10 — `int[]`; выравнен по 32 бита; публичный interop через `int[]`) |
| Интерфейсы | `ICollection`, `IEnumerable` (non-generic), `ICloneable` |
| Итерация | `GetEnumerator()` → **bool-значения** (не индексы set-битов!) |
| Операторы | indexer `this[int]` только; **нет** `&`, `\|`, `^`, `~` |
| Bulk-операции | `And`, `Or`, `Xor`, `Not` — in-place, возвращают `this` (chaining); `And`/`Or`/`Xor` **требуют одинаковых длин**; `Not()` — unary, без size-matching контракта |
| Отсутствует | `AndNot`, `Intersects`, навигация по битам, range operations, value equality |
| Конвертации | конструкторы из `byte[]`, `bool[]`, `int[]`; `CopyTo(Array, int)`; `CollectionsMarshal.AsBytes(BitArray)` → zero-copy `Span<byte>` над байтами текущего `Length` (.NET 10) |
| Shift | `LeftShift(n)`, `RightShift(n)` (с .NET Core 2.0) |
| Cardinality | `PopCount()` — **с .NET 10** |
| Any/All предикаты | `HasAllSet()` / `HasAnySet()` — с .NET 8 (возвращают `bool`, не count) |
| Equals/Hash | **не переопределены** (reference equality, identity hash) |
| ToString | возвращает `"System.Collections.BitArray"` |

**Антипаттерны:** C# `BitArray` — пример устаревшего дизайна, который за 25 лет добавил лишь 5 новых методов. Ключевые проблемы: нет value equality, итерация по bool (не по индексам set-битов), strict size matching для bulk ops, нет навигации, non-generic `IEnumerable` без `IEnumerable<bool>` (object-typed enumeration с unboxing path; per-element boxing аллокаций нет — runtime кэширует singleton-ы). Единственные полезные идеи: indexer, chaining (return `this`), shift-операции, конструктор с default value.

### `System.Collections.Specialized.BitVector32`

Отдельная `struct` (value type) для ровно 32 бит. В типичных unboxed сценариях обходится без отдельной heap-аллокации, но не является `ref struct` и не имеет гарантии stack-only размещения. Два способа адресации: bit flags по маскам и packed integers по секциям (смешивать оба на одном layout не рекомендуется, но section-based layout поддерживает `Section(maxValue: 1)` для булевых значений). Не является альтернативой `BitArray` для общих задач — специализация для performance-critical кода с известным ≤32 бит.

---

## 5. Scala

Детальный анализ: [`step-02-scala-bitset.md`](step-02-scala-bitset.md). Baseline: Scala 2.13.18 (git tag `v2.13.18`).

### `scala.collection.immutable.BitSet` и `scala.collection.mutable.BitSet`

| Аспект | `immutable.BitSet` | `mutable.BitSet` |
|---|---|---|
| Тип | `sealed abstract class` | `class` |
| Супертипы | `AbstractSet[Int]`, `SortedSet[Int]`, `Serializable` | `AbstractSet[Int]`, `SortedSet[Int]`, `Serializable` |
| Хранение | inline `Long` fields или `Array[Long]` (small-set specialization; observed 2.13.18 source implementation detail) | `var elems: Array[Long]` |
| Мутация | возвращает новый BitSet (`+`, `-`, `\|`, `&`, `^`, `&~`) | модифицирует in-place (`+=`, `-=`, `\|=`, `&=`, `^=`, `&~=`) |
| Размер | динамический, автоматическое уменьшение представления (observed impl detail) | динамический, удваивает массив; `clear()` удаляет все элементы (гарантия сохранения capacity не документирована) |
| Интерфейсы | **полный `Set[Int]` + `SortedSet[Int]` + `Function1[Int, Boolean]`** | аналогично + `Growable`, `Shrinkable` |
| Итерация | `iterator`, `foreach`, for-comprehensions, `stepper` (Java stream interop) | аналогично |
| Операторы | `+`, `-`, `--`, `++`, `&`, `\|`, `^`, `&~` | `+=`, `-=`, `++=`, `--=`, `&=`, `\|=`, `^=`, `&~=` + все из immutable |
| Конвертации | `toBitMask: Array[Long]`, `fromBitMask(Array[Long])`, все стандартные (`toList`, `toSet`, `toArray`, ...) | + `toImmutable`, `clone()` |
| Map/flatMap | `map(Int => Int)` возвращает `BitSet`; `map(Int => B)` → `SortedSet[B]` | аналогично |

**Уникальные решения:**

1. **`Set[Int]` + `SortedSet[Int]`** — богатейшая интеграция с коллекциями из всех языков. BitSet = полноценное множество целых чисел, работающее со всей collections API: `map`, `flatMap`, `filter`, `foldLeft`, `zip`, for-comprehensions. `Set[A]` расширяет `A => Boolean`, поэтому BitSet используется как предикат: `list.filter(bitSet)`.

2. **Immutable/Mutable split через отдельные классы** — единственный язык с полноценным разделением. Общий трейт `collection.BitSet` с `BitSetOps` определяет shared-операции. Immutable возвращает новые объекты, mutable модифицирует in-place.

3. **Small-set optimization** (observed impl detail, 2.13.18 source) — immutable-вариант хранит малые множества в inline `Long`-полях без аллокации массива (≤64 бит — 1 Long, ≤128 бит — 2 Long). Автоматическое уменьшение представления при операциях. Реализовано через internal implementation classes (deprecated in public API docs).

4. **Нет `flip`/`negate`, нет `nextSetBit`** — осознанное решение. Scala не экспонирует низкоуровневые bit-операции; все делается через set-операции и итерацию.

---

## 6. Swift

Детальный анализ: [`step-02-swift-bitset.md`](step-02-swift-bitset.md).

### `BitSet` из swift-collections

| Аспект | Значение |
|---|---|
| Тип | `public struct BitSet` (value type) |
| Пакет | swift-collections `1.3.0` (analysis baseline; released September 2025); НЕ часть стандартной библиотеки |
| Мутабельность | value type с CoW; mutating methods помечены `mutating` |
| Размер | динамический (auto-grow) |
| Внутреннее представление | `[_Word]` (обёртка над `UInt`, platform word size) |
| Протоколы | `SetAlgebra`, `Sequence`, `Collection`, `BidirectionalCollection`, `Equatable`, `Hashable`, `Codable` (non-embedded), `ExpressibleByArrayLiteral`, `Sendable` |
| Элемент | `Int` (множество неотрицательных целых) |
| Set algebra | `union`, `intersection`, `symmetricDifference`, `subtracting`, `formUnion`, `formIntersection`, `formSymmetricDifference`, `subtract`; `isSubset`, `isSuperset`, `isStrictSubset`, `isStrictSuperset`, `isDisjoint`, `isEqualSet` — по 4 overloads каждый (BitSet, BitSet.Counted, Range, Sequence) = **56 методов** |
| Итерация | `Sequence`/`Collection` — итерация по member values (`Int`), в ascending order |
| Subscripts | `subscript(member: Int)` → `Bool` (get/set/toggle); `subscript(members: Range<Int>)` → `Slice` |
| Операторы | **нет** bitwise operators; только `==` |
| Конвертации | `init(words:)`, `init(bitPattern:)`, `init(_ range: Range<Int>)`, `Codable` (UInt64-based) |
| Counted | `BitSet.Counted` — обёртка с кешированным `count` для O(1) `count`/`isEmpty` |
| Concurrency interop | `Sendable` (value type; безопасная передача между isolation domains, не эквивалент синхронизации shared mutable state) |
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
| C++ | `boost::dynamic_bitset` | Runtime, явный | **Нет** (explicit resize; OOB: `at()` → `out_of_range`, остальные — assert) | `resize`, `push_back`, `pop_back`, `push_front`/`pop_front` (1.90.0+), `append` | `reserve`, `capacity`, `shrink_to_fit` |
| Rust | `bitvec` (`BitVec`) | Динамический | Да (через `push`) | `resize`, `truncate` | `reserve`, `capacity`, `shrink_to_fit` |
| Rust | `fixedbitset` | Фикс. + grow | Нет (panic при OOB) | `grow()` (только рост); неявный рост в `union_with` / `symmetric_difference_with` | Нет |
| C# | `BitArray` | Фикс. + explicit resize | Нет (exception при OOB) | `Length` setter | Нет |
| Scala | оба варианта | Динамический | **Да** (при `+`/`+=`) | Автоматически | Нет публичного capacity API; рост массива управляется внутренне (`ensureCapacity` — `protected`) |
| Swift | `BitSet` | Динамический | **Да** (при `insert`) | Автоматически | Preallocation only: `reserveCapacity(maximumValue)` (нет `capacity` getter) |

**Тенденции:**
- Auto-grow (Java, Scala, Swift) — наиболее удобная модель для пользователей.
- C++ boost и Rust fixedbitset требуют явного управления размером — более безопасно, но менее удобно. Для boost явный resize необходим не только для OOB-записи, но и как подготовка к бинарным/set-операциям (все требуют `size() == rhs.size()`).
- C# — гибрид: фиксированный, но с settable `Length` (awkward API).
- Для Kotlin: auto-grow наиболее ожидаем (совместимо с Java `BitSet` и паттерном коллекций Kotlin).

### 7.3 Интерфейсы коллекций

| Язык | Реализация | Set | SortedSet | Iterable / Collection | Function / Predicate |
|---|---|---|---|---|---|
| Java | `BitSet` | **Нет** | Нет | **Нет** (только `stream()`) | Нет |
| C++ | `std::bitset` | Нет | Нет | **Нет** | Нет |
| C++ | `boost` (1.90.0+) | Нет | Нет | Итераторы `begin`/`end`/`rbegin`/`rend` (Boost 1.90.0+) | Нет |
| Rust | `bitvec` | Нет | Нет | `IntoIterator` (owned: `bool`; borrowed: proxy refs `BitRef`), `FromIterator<bool>` / `<T>`, `Extend<bool>` / `<T>` | Нет |
| Rust | `fixedbitset` | Нет (но set-API) | Нет | `FromIterator<usize>`, `Extend<usize>`; именованные итераторы `ones()` / `into_ones()` (нет trait-level `IntoIterator`) | Нет |
| C# | `BitArray` | Нет | Нет | `ICollection`, `IEnumerable` (non-generic) | Нет |
| Scala | `BitSet` | **`Set[Int]`** | **`SortedSet[Int]`** | **Полная collections API** | **`Int => Boolean`** |
| Swift | `BitSet` | **`SetAlgebra`** | Нет (но sorted by nature) | **`BidirectionalCollection`** | Нет |

**Тенденции:**
- Java и `std::bitset` (C++) **не** интегрируют BitSet в свои коллекционные фреймворки — это главный источник жалоб и boilerplate. C# `BitArray` реализует `ICollection`/`IEnumerable`, но это минимальная non-generic интеграция (без `IEnumerable<bool>`, без `ISet<int>`). `boost::dynamic_bitset` с Boost 1.90.0 добавил поддержку итераторов (пригодных для C++20 ranges, но не удовлетворяющих `LegacyForwardIterator`), формальный Container по-прежнему не заявлен.
- Scala — максимальная интеграция: `Set[Int]` + `SortedSet[Int]`, полная collections API, BitSet как `Function1[Int, Boolean]`.
- Swift — сильная интеграция через `SetAlgebra` + `Collection`.
- Rust — средняя: trait implementations (`IntoIterator`, `FromIterator`) обеспечивают совместимость с экосистемой, но нет формального `Set` trait.
- Для Kotlin: реализация `Set<Int>` или хотя бы `Iterable<Int>` — ключевое ожидание пользователей. Scala-подход наиболее близок к паттерну коллекций Kotlin.

### 7.4 Итерация

| Язык | Реализация | По set-битам (индексы) | По всем битам (bool) | По clear-битам | Обратная итерация | Навигация |
|---|---|---|---|---|---|---|
| Java | `BitSet` | `stream()` (IntStream) | Нет | через `nextClearBit()`/`previousClearBit()` | через `previousSetBit()`/`previousClearBit()` | `nextSetBit`, `nextClearBit`, `previousSetBit`, `previousClearBit` |
| C++ | `std::bitset` | **Нет** | Ручной цикл | Ручной цикл | Нет | **Нет** |
| C++ | `boost` (1.90.0+) | `find_first(pos)`/`find_next(pos)` | Итераторы `begin`/`end` (Boost 1.90.0+, обходят все позиции) | `find_first_off(pos)`/`find_next_off(pos)` (Boost 1.90.0+) | **Да** (`rbegin`/`rend`, Boost 1.90.0+) | `find_first(pos=0)`, `find_next`, `find_first_off(pos=0)`, `find_next_off` |
| Rust | `bitvec` | `iter_ones()` | `iter()` (proxy refs `BitRef`; адаптеры `.by_refs()` / `.by_vals()`) | `iter_zeros()` | `DoubleEndedIterator` | `first_one`, `first_zero`, `last_one`, `last_zero` |
| Rust | `fixedbitset` | `ones()`, `into_ones()`; ленивые set-algebra: `intersection`, `union`, `difference`, `symmetric_difference` | Нет | `zeroes()` (только forward) | `DoubleEndedIterator` (для `ones()`, `into_ones()` и lazy set-algebra iterators) | `minimum()`, `maximum()` |
| C# | `BitArray` | **Нет** | `GetEnumerator()` (bool) | Нет | Нет | **Нет** |
| Scala | `BitSet` | `iterator`, `foreach`, for-comprehensions | Нет (только set-биты) | Нет | Нет | `min`, `max`, `iteratorFrom`, `range`/`from`/`until` |
| Swift | `BitSet` | `makeIterator()`, for-in | Нет (только set-биты) | Нет | **Да** (`BidirectionalCollection`) | `first`, `last`, `index(after:)`, `index(before:)` |

**Тенденции:**
- Итерация **по индексам set-битов** — доминирующий паттерн (Java, Rust, Scala, Swift). C# — антипаттерн (итерирует bool-значения).
- Итерация по clear-битам — редка (Java через `nextClearBit()`/`previousClearBit()`, bitvec, fixedbitset, `boost::dynamic_bitset` 1.90.0+), но полезна для некоторых алгоритмов.
- Обратная итерация / bidirectional — поддерживается в Swift, Rust (DoubleEndedIterator), `boost::dynamic_bitset` 1.90.0+ (`rbegin`/`rend`), частично в Java (`previousSetBit`/`previousClearBit`).
- `minimum()`/`maximum()` (fixedbitset) — более чистый API, чем Java `nextSetBit(0)` / цикл.
- Scala `iteratorFrom(start)` — мощный примитив для SortedSet.
- Для Kotlin: `Iterable<Int>` / `Iterator<Int>` по set-битам — ожидаемый минимум. `forEachBit {}` (inline) — для производительности.

#### Итерация / проекции по диапазону

| Язык | Реализация | Start-from / lower-bound primitive | Range projection / slicing | Chunks / windows |
|---|---|---|---|---|
| Java | `BitSet` | `nextSetBit(fromIndex)`, `nextClearBit(fromIndex)` | `get(fromIndex, toIndex)` → new `BitSet` (eager copy, не view) | Нет |
| C++ | `std::bitset` | Нет | Нет | Нет |
| C++ | `boost` | `find_first(pos)`, `find_next(pos)`, `find_first_off(pos)`, `find_next_off(pos)` | Нет | Нет |
| Rust | `bitvec` | Нет (endpoint queries `first_one()` и др. — whole-slice, без входного lower bound; start-from через `[range]` + итерацию) | `[range]` → `&BitSlice` / `&mut BitSlice` (read и mutable, через `IndexMut<Range*>`); `split_at()` / `split_at_mut()` | `chunks()`, `chunks_exact()`, `rchunks()`, `windows()` + mutable варианты |
| Rust | `fixedbitset` | Нет (endpoint queries: `minimum()`, `maximum()` — глобальные, без входного lower bound) | Нет | Нет |
| C# | `BitArray` | Нет | Нет | Нет |
| Scala | `BitSet` | `iteratorFrom(start)` | `range(from, until)`, `from(elem)`, `until(elem)`, `rangeTo(to)` — возвращают новый `BitSet` | Нет |
| Swift | `BitSet` | Нет (Collection navigation `index(after:)` / `index(before:)` — по opaque `Index`, не lower-bound API) | `subscript(members: Range<Int>)` / `subscript(members: some RangeExpression<Int>)` → `Slice<BitSet>` | Нет |

#### Range mutation / range predicates

Отдельная ось от range projection / slicing: операции **мутации** и **предикатов** над диапазонами, не требующие получения view/copy подмножества.

| Язык | Реализация | Range mutation | Range predicates |
|---|---|---|---|
| Java | `BitSet` | `set(from, to)`, `clear(from, to)`, `flip(from, to)` | Нет |
| C++ | `std::bitset` | Нет | Нет |
| C++ | `boost` | `set(pos, len)`, `reset(pos, len)`, `flip(pos, len)` | Нет |
| Rust | `bitvec` | Через mutable sub-slice: `[range]` → `&mut BitSlice` | Через sub-slice: `count_ones()`, `any()`, `all()` на `&BitSlice` |
| Rust | `fixedbitset` | `insert_range`, `remove_range`, `toggle_range`, `set_range` | `contains_all_in_range`, `contains_any_in_range`, `count_ones(range)`, `count_zeroes(range)` |
| C# | `BitArray` | Нет | Нет |
| Scala | `BitSet` | Нет (через set algebra, создающую новый `BitSet`) | Нет |
| Swift | `BitSet` | Set algebra с `Range<Int>` overloads: `formUnion(range)`, `subtract(range)` и др. | Set relation с `Range<Int>` overloads: `isSubset(of: range)`, `isDisjoint(with: range)` и др. |

**Тенденции:**
- **Dedicated start-from / lower-bound primitive** (метод с параметром начальной позиции) есть только в Java (`nextSetBit(fromIndex)`), Boost (`find_first(pos)` / `find_next(pos)`) и Scala (`iteratorFrom(start)`). В Rust и Swift аналогичный результат достигается композицией: slicing + endpoint queries (`bitvec`) или Collection navigation по opaque `Index` (Swift).
- **Endpoint queries** (глобальный min/max без входного параметра) — отдельная категория, уже отражённая в основной таблице итерации: `bitvec` (`first_one()` / `last_one()` и др.), `fixedbitset` (`minimum()` / `maximum()`), Swift (`first` / `last`), Scala (`min` / `max`). Эти методы не являются аналогом `nextSetBit(fromIndex)`.
- **Range projection / extraction** — четыре подхода: eager-copy (Java `get(fromIndex, toIndex)` → new `BitSet`), slice-based (Rust `bitvec` `[range]` → `&BitSlice` / `&mut BitSlice`, включая mutable range slicing через `IndexMut<Range*>`), SortedSet-based (Scala `range`/`from`/`until` → новый `BitSet`), collection subscript (Swift `subscript(members:)` → `Slice<BitSet>`). Boost, `fixedbitset`, C# — не предоставляют range projection.
- **Range mutation** — самостоятельная ось, не сводящаяся к range projection. Java (`set`/`clear`/`flip` с `(from, to)`), Boost (`set`/`reset`/`flip` с `(pos, len)`) и `fixedbitset` (`insert_range`/`remove_range`/`toggle_range`/`set_range`) предоставляют dedicated range-мутации. Swift покрывает через `Range<Int>` overloads на set algebra методах (`formUnion(range)`, `subtract(range)` и др.).
- **Range predicates** — предикаты над диапазонами без создания промежуточного view/copy. `fixedbitset` (`contains_all_in_range`/`contains_any_in_range`, `count_ones(range)`/`count_zeroes(range)`), Swift (set relation с `Range<Int>` overloads: `isSubset(of: range)`, `isDisjoint(with: range)` и др.), bitvec (через sub-slice: `count_ones()`, `any()`, `all()` на `&BitSlice`).
- **Chunks / windows** — уникальны для `bitvec`, где BitSlice моделирует полноценный аналог `&[bool]` с соответствующим slice API. Остальные реализации этот уровень абстракции не предоставляют.
- Для Kotlin: start-from навигация (`nextSetBit(fromIndex)`) — обязательный минимум. Endpoint queries (`minimum()` / `maximum()`) — более чистый API для частого случая. Range projection — дизайн-вопрос (шаг 8): view vs copy, lazy vs eager.

### 7.5 Операторы и синтаксический сахар

| Язык | `[]` get | `[]` set | `&`/`\|`/`^` | `~`/`!` (NOT) | Set diff op | Shift `<<`/`>>` | `in` / contains op | Literal / factory shorthand |
|---|---|---|---|---|---|---|---|---|
| Java | `get(i)` | `set(i)` | Нет | Нет | Нет | Нет | Нет | Нет |
| C++ | `[]` / `test`; boost: `at()` (checked, `out_of_range`) | `[]` = proxy; boost: `at()` = checked proxy | `&=` `\|=` `^=` + free | `~` | `-` (boost) | `<<=` `>>=` `<<` `>>` | Нет | Нет |
| Rust (bitvec) | `[]` → bool (read-only) | `set()` (бит); `[range]` → `&mut BitSlice` | `&` `\|` `^` | `!` | Нет | `<<` `>>` | Нет | `bitvec![...]` macro |
| Rust (fixedbitset) | `[]` → bool | `set()`, `insert()` | `&` `\|` `^` (non-mutating: `&` → `min(len)`, `\|`/`^` → `max(len)`) | **Нет** | Нет | Нет | `contains()` | Нет |
| C# | `this[i]` | `this[i]=` | Нет | Нет | Нет | Методы | Нет | Нет |
| Scala (immutable) | `apply(i)` → Bool | `+`/`-` (возвр. новый) | `&` `\|` `^` | Нет | `&~` | Нет | `contains`, `apply` | `BitSet(1, 3, 5)` |
| Scala (mutable) | `apply(i)` → Bool | `update(i, Bool)` | `&` `\|` `^` + `&=` `\|=` `^=` | Нет | `&~`, `&~=` | Нет | `contains`, `apply` | `mutable.BitSet(1, 3, 5)` |
| Swift | `[member: i]` | `[member: i]=` | Нет | Нет | Нет | Нет | `contains(i)` | `[1, 3, 5]` literal |

**Тенденции:**
- `[]` для чтения — универсально (кроме Java из-за языковых ограничений).
- `[]` для записи — C++ (proxy), C# (indexer), Swift (`subscript(member:)`), Scala (`mutable.BitSet.update`; immutable: операторы `+`/`-`).
- Bitwise operators — C++ и Rust имеют полный набор; Scala использует set-теоретические операторы (`&`, `|`, `^`, `&~`); Swift и Java — без операторов.
- NOT (`~`/`!`) — требует явной конечной границы инверсии. Определён для `std::bitset<N>` (compile-time N), `boost::dynamic_bitset` (runtime `size()`), `bitvec` (runtime `len()`). Отсутствует в `fixedbitset`, Java, Scala, Swift — где граница неочевидна.
- Set difference как оператор: C++ boost (`-`), Scala (`&~`) — удобно.
- Предусловие совпадения размеров: C++ boost и C# `BitArray` требуют одинаковых размеров для бинарных операций (assert/exception при несовпадении); Java, Scala, Swift допускают разные размеры. `fixedbitset` — промежуточный вариант: немутирующие операторы (`&`, `|`, `^`) допускают разные длины, но результат имеет асимметричную длину: `&` → `min(a.len(), b.len())`, `|`/`^` → `max(a.len(), b.len())`.
- Для Kotlin: `operator get`, `operator set`, infix `and`/`or`/`xor` — наиболее идиоматично. `operator contains` (`in`) — ожидаем.

### 7.6 Сериализация и interop

| Язык | В raw массив | Из raw массива | Из коллекций | В коллекции | В строку | Стандартная сериализация | Кросс-платформенная |
|---|---|---|---|---|---|---|---|
| Java | `toLongArray()`, `toByteArray()` | `valueOf(long[])`, `valueOf(byte[])` | Нет | Нет (только `stream()`) | `{0, 2, 5}` | `Serializable` | Нет |
| C++ std | Нет (`to_ulong()` / `to_ullong()` — scalar conversion, не array) | Конструктор из `ulong` (scalar) | Нет | Нет | `to_string()` `"01010"` (binary, MSB-first) | Нет | Нет |
| C++ boost | `to_block_range()` | `from_block_range()`, конструктор | Нет | Нет | non-member `to_string(b, s)` `"01010"` (binary, MSB-first) | Boost.Serialization (optional, отд. header) | Нет |
| Rust (bitvec) | Low-level raw storage: `as_raw_slice()`, `into_vec()`; dead bits unspecified — для детерминированного экспорта нужны `set_uninitialized()` и при необходимости `force_align()` | `from_vec()`, `from_slice()` (panic on overflow); fallible: `try_from_vec()`, `try_from_slice()` | `FromIterator<bool>`, `FromIterator<T>` (raw storage) | Нет dedicated conversion API; типичный путь — `into_iter().collect::<Vec<bool>>()` (через trait-level `IntoIterator`) | `Display`: list-style `[0, 1, 1, 0, 1]`; `Binary`: `01101` | Serde (optional); `std::io::Read`/`Write` (feature `std`) | Через serde |
| Rust (fixedbitset) | `as_slice()` | `with_capacity_and_blocks()` | `FromIterator<usize>` | Нет direct conversion API; типичный путь — `ones().collect()` / `into_ones().collect()` | Binary `01101010` (LSB-first) | Serde (optional) | Через serde |
| C# | `CopyTo(Array, int)` → `int[]` / `byte[]` (runtime dispatch, единый метод); `CollectionsMarshal.AsBytes(BitArray)` → zero-copy `Span<byte>` над байтами текущего `Length`, не полный capacity export (.NET 10) | Конструктор `int[]`, `byte[]`, `bool[]` | Нет | `CopyTo(Array, int)` → `bool[]` | `"System.Collections.BitArray"` | `[Serializable]` атрибут; `ISerializable` не экспонируется в публичном API, но в runtime source .NET 10 реализован для обратной совместимости со старыми `BinaryFormatter`-данными (deprecated .NET 5+, disabled most project types .NET 8, removed .NET 9) | Нет |
| Scala | `toBitMask: Array[Long]` | Companion-object factories: `fromBitMask(Array[Long])`, `fromBitMaskNoCopy(Array[Long])` (на обоих вариантах); mutable: публичный конструктор `new BitSet(Array[Long])` (zero-copy) | Companion `fromSpecific(IterableOnce[Int])` (основной путь из коллекции); также `apply(Int*)` (varargs shorthand) | `toList`, `toSet`, `toArray`, ... | `"BitSet(1, 3, 5)"` (observed, not guaranteed by API) | `Serializable` | Нет (JVM-only) |
| Swift | Нет прямого | `init(words:)` (из `Sequence<UInt>`); отдельно `init(bitPattern:)` (scalar `BinaryInteger`, не array) | `init(_ sequence:)` | `Array(bitSet)` через Collection | `"[1, 2, 3]"` | `Codable` (UInt64; non-embedded builds) | **Да** (UInt64-based; non-embedded builds) |

**Тенденции:**
- Word/block-level raw interop встречается в нескольких дизайнах, но формы различаются: Java даёт `toLongArray()` / `valueOf(long[])` (статическая фабрика, а не `fromLongArray()`), Scala — `toBitMask` / `fromBitMask`, Boost — generic block-range API (`to_block_range()` / `from_block_range()`, зависит от шаблонного `Block`). `std::bitset` ограничен scalar conversions (`to_ulong()`, `to_ullong()`) и строками — raw-array interop отсутствует.
- Swift `Codable` с UInt64-словами — лучший подход к кросс-платформенной сериализации (доступен только в non-embedded builds).
- Конвертации в/из коллекций (`toList`, `toSet`, `FromIterator`) — ключевой пробел Java, хорошо покрыт в Scala и Rust.
- `toString()` формат — три варианта: **set-style** (`{0, 2, 5}` — Java), **list/array-style** (`[1, 2, 3]` — Swift; `BitSet(1, 3, 5)` — Scala, observed, без API guarantee; `bitvec` `Display`: `[0, 1, 1, 0]`), **binary** (`"01010"` — C++, `fixedbitset`). `bitvec` предоставляет бинарную строку через отдельный `Binary` formatter, но его default `Display` — list-style. Внутри бинарного формата важно различать порядок бит: C++ `std::bitset` и Boost `to_string()` печатают MSB-first (младший бит = правый символ), тогда как `fixedbitset` печатает LSB-first (проход `for i in 0..length`, бит 0 = левый символ).
- Для Kotlin: raw long-array import/export API (export: `toLongArray()`; import: фабрика из `LongArray` — конкретное имя, например `fromLongArray()`, определяется на шаге 8) + `toSet()`/`toBitSet()` extension — минимум. `kotlinx.serialization` support — отдельный вопрос (шаг 8c).

---

## 8. Сводка дизайн-решений по языкам

### Ключевые архитектурные решения

| Решение | Java | C++ std | C++ boost | Rust bitvec | Rust fixedbitset | C# | Scala | Swift |
|---|---|---|---|---|---|---|---|---|
| **Концепт. модель** | Bit vector (с set-like ops) | Bit vector | Bit vector | **Sequence of bits** | **Set-like API** (fixed-length bitmap) | Bit array | **Set of integers** | **Set of integers** |
| **Мутабельность** | Mutable | Mutable | Mutable | Ownership | Mutable | Mutable | **Immutable + Mutable** | Value type (CoW) |
| **Размер** | Dynamic auto | Fixed compile | Dynamic explicit | BitVec: dynamic; BitArray/BitBox: fixed | Fixed + grow | Fixed + resize | Dynamic auto | Dynamic auto |
| **Коллекции** | Нет | Нет | Итераторы (1.90.0+) | Traits | Traits | Non-generic | **Full Set[Int]** | **SetAlgebra + Collection** |
| **Итерация set** | stream() | — | find_first(pos)/next | iter_ones() | ones() + lazy set-algebra iters | — | iterator/foreach | Collection iteration |
| **Cardinality** | `cardinality()` | `count()` | `count()` | `count_ones()` | `count_ones(..)` | `PopCount()` (.NET 10) | `size` | `count` (O(max)) |
| **Value equality** | Да | Да | Да | Да | Да (length-sensitive; trailing zeros matter) | **Нет** | Да | Да |
| **Internal repr** | `long[]` | impl-defined | Block-based (backend configurable via `AllocatorOrContainer`) | Generic `T` | SIMD-backed blocks; public block type = `usize` | `byte[]` (.NET 10; до .NET 10 — `int[]`) | immutable: inline `Long` fields or `Array[Long]` (small-set specialization; observed 2.13.18 source impl detail); mutable: `Array[Long]` | `[UInt]` |

### Уникальные идеи каждого языка

| Язык | Идея | Релевантность для Kotlin |
|---|---|---|
| Scala | `Set[Int]` + `SortedSet[Int]`, immutable/mutable split, small-set optimization (observed source-level impl detail, 2.13.18), `apply(i)` = `contains`, type-preserving `map` | **Высокая** — паттерн коллекций Kotlin аналогичен Scala |
| Swift | `SetAlgebra`, `BitSet.Counted`, `subscript(member:)` с toggle, отдельный `BitArray`, `Codable` с UInt64, `ExpressibleByArrayLiteral`, overloads для Range/Sequence | **Высокая** — современный дизайн, value-type семантика |
| Rust (fixedbitset) | Три слоя set-операций (in-place `*_with` / counting `*_count` / lazy iterators `intersection`/`union`/`difference`/`symmetric_difference`), `minimum()`/`maximum()`, SIMD, `grow_and_insert`, range mutation (`insert_range`/`remove_range`/`toggle_range`/`set_range`), range predicates (`contains_all_in_range`/`contains_any_in_range`, `count_ones(range)`/`count_zeroes(range)`) | **Средняя** — полезные оптимизации и design patterns |
| Rust (bitvec) | Deref delegation (`BitVec → BitSlice`), `BitOrder` parametrization, `iter_ones()`/`iter_zeros()`, chunks/windows/split | **Средняя** — delegation паттерн полезен; bit order — скорее нет |
| C++ boost | `find_first(pos=0)`/`find_next` + `npos` sentinel, `operator-` (set diff), `is_subset_of`/`is_proper_subset_of`, `test_set` (get+set), block-level interop, итераторы `begin`/`end`/`rbegin`/`rend`, `find_first_off`/`find_next_off`, `push_front`/`pop_front` (Boost 1.90.0+). NB: богатый operator surface сочетается с `same-size` контрактом (все бинарные/set-операции требуют совпадения размеров) — менее эргономично, чем auto-grow Java | **Средняя** — хорошие API решения |
| C# | Indexer `this[int]`, in-place bulk ops return `this` (chaining), конструктор с default value, shift-операции, `BitVector32` для ≤32 бит | **Низкая** — в основном антипаттерны |

---

## 9. Ключевые выводы и тенденции

### 9.1 Фундаментальный выбор: «множество целых» vs «вектор бит»

Два подхода к абстракции:
- **«Множество целых чисел»** (Scala `Set[Int]`, Swift `SetAlgebra`): `contains(5)`, `insert(5)`, `remove(5)`, итерация по членам, set-compatible семантика равенства. Rust `fixedbitset` использует set-like API и итерацию по членам, но не имеет set-compatible семантики равенства: derived `Eq`/`Hash` зависят от длины, а не только от множества set-битов.
- **«Вектор бит»** (Rust `bitvec`, C++ `bitset`, C# `BitArray`): `get(i)` / `set(i, bool)` — позиционный доступ по индексу. Итерация варьируется: `bitvec` — `iter()` по всем битам через proxy refs (`BitRef`; для `&bool` / `bool` — адаптеры `.by_refs()` / `.by_vals()`), отдельно `iter_ones()`/`iter_zeros()` по индексам; C# `BitArray` — `GetEnumerator()` по всем битам (bool), нет итерации по индексам; `std::bitset` — итераторов нет, только ручной цикл.

Java `BitSet` — гибрид: API в стиле «вектор бит» (`get`/`set`/`clear`), но `stream()` возвращает индексы set-битов (set-семантика). Современные реализации тяготеют к модели «множество». Swift явно разделяет эти модели на `BitSet` и `BitArray`.

**Рекомендация для Kotlin:** модель «множество целых» лучше интегрируется с Kotlin collections (`Set<Int>`, `Iterable<Int>`), но bit-level API (`set`, `clear`, `flip`, `get`) необходим для совместимости с Java и low-level use cases. Оптимальный подход: set-like API первичен (интерфейсы, итерация), bit-level API — как дополнение.

### 9.2 Интеграция с коллекциями — ожидание пользователей

Спектр интеграции (от минимальной к максимальной):
1. **Нет интеграции** — Java, `std::bitset` (C++)
2. **Минимальная non-generic интеграция** — C# (`ICollection`/`IEnumerable`, без generic-версий; исторический дизайн)
3. **Итераторы без формального Container** — `boost::dynamic_bitset` (Boost 1.90.0+: `begin`/`end`/`rbegin`/`rend`; C++20 ranges, не LegacyForwardIterator)
4. **Trait/protocol conformance** — Rust (`IntoIterator`, `FromIterator`)
5. **Collection protocol** — Swift (`SetAlgebra`, `Collection`)
6. **Полная коллекционная иерархия** — Scala (`Set[Int]`, `SortedSet[Int]`, `Iterable`, `map`/`flatMap`/`filter`)

Тенденция: современные дизайны (Swift 2022, Scala 2.13) стремятся к максимальной интеграции. Пользователи ожидают `for (bit in bitSet)`, `bitSet.filter { ... }`, `5 in bitSet`.

### 9.3 Mutable/immutable split

Три подхода:
1. **Только мутабельный** — Java, C++, C#. Самый простой, но ограничивает safety и functional-style код.
2. **Отдельные классы** — Scala. Максимальная гибкость, но сложнее API.
3. **Value type с CoW** — Swift. Естественная иммутабельность через семантику значений.

Для Kotlin наиболее естественен подход, совместимый с паттерном `List`/`MutableList`: read-only interface (`BitSet`) + mutable implementation (`MutableBitSet`).

### 9.4 NOT/Complement проблема

Операция «побитовое дополнение» семантически неоднозначна для множеств без явной конечной границы инверсии. Три случая:
- **Compile-time fixed boundary:** `std::bitset<N>` — `~` определён, граница инверсии = `N`.
- **Runtime explicit boundary:** `boost::dynamic_bitset` (`operator~`, граница = `size()`), `bitvec` (`!`, граница = `len()`). Complement корректно определён для runtime-sized типов, если текущая длина задана явно.
- **Нет канонической глобальной границы:** set-like API (`fixedbitset`, Java, Scala, Swift) — безопаснее range-based `flip`.
  - `fixedbitset`: **нет** `Not` — осознанное решение (инвертировать до какой границы?).
  - Java: нет общего `not()`, но есть `flip(from, to)` для диапазонов.
  - Scala: нет `~`/`not`.
  - Swift: нет complement.

Для Kotlin: `flip(index)`, `flip(range)` — безопасны. Глобальный `not()` / `operator inv()` — проблематичен без явной конечной границы.

### 9.5 Именование: cardinality/count/size/popcount

| Язык | «Кол-во set-битов» | «Общий размер» | «Ёмкость» |
|---|---|---|---|
| Java | `cardinality()` | `length()` (MSB+1), `size()` (capacity) | `size()` |
| C++ | `count()` | `size()` | — / `capacity()` (boost) |
| Rust (bitvec) | `count_ones()` | `len()` | `capacity()` |
| Rust (fixedbitset) | `count_ones(..)` (range) | `len()` | — (нет public getter) |
| C# | `PopCount()` (.NET 10) | `Length` / `Count` | — |
| Scala | `size` (от `Set`) | — | — |
| Swift | `count` (от `Collection`) | — | — (preallocation: `reserveCapacity(maximumValue)`, нет capacity getter) |

`cardinality` — Java-specific термин. Если BitSet реализует `Set<Int>`, `size` = число set-битов (как в Scala, Swift). `count` — наиболее распространённое имя для popcount (C++, Rust, Swift).

**Семантика пустоты.** Отдельный нюанс — различие между «структурно пустой» (нулевая длина) и «нет установленных битов» (все биты сброшены). В языках с auto-grow (Java, Scala, Swift) эти понятия фактически совпадают: Java `isEmpty()` означает «нет set-битов» (при этом `length() == 0`), аналогично Scala `isEmpty` (от `Set`) и Swift `isEmpty` (от `Collection`). В C# `BitArray`, где размер фиксирован, это два разных предиката: `Length == 0` (структурная пустота) vs `HasAnySet() == false` (.NET 8+, нет set-битов). `fixedbitset` и `boost::dynamic_bitset` разводят это явно через отдельные публичные предикаты: `fixedbitset` — `is_empty()` (`len() == 0`, структурная пустота) vs `is_clear()` (все биты сброшены); `boost` — `empty()` (`size() == 0`, структурная пустота) vs `none()` (`count() == 0`, нет set-битов). Пример: bitset со 100 нулевыми битами — `is_empty()`/`empty()` = false, `is_clear()`/`none()` = true. Это различие существенно для дизайна (шаг 8): если Kotlin BitSet реализует `Set<Int>`, `isEmpty` должен означать «нет set-битов» (set-семантика), а не «нулевая длина».

### 9.6 Итерация: set-биты как первичный паттерн, range-oriented API как дизайн-сигнал

Все современные реализации (Scala, Swift, Rust fixedbitset) итерируют **по set-битам** (возвращая индексы `Int`), а не по всем битам. C# `BitArray` — антипаттерн (итерирует bool-значения). Java `stream()` — правильный подход, но ограничен Java 8+ API.

Отдельный дизайн-сигнал — range-oriented API. Dedicated start-from / lower-bound primitives (метод с параметром начальной позиции для поиска следующего set-бита) есть только в Java (`nextSetBit(fromIndex)`), Boost (`find_first(pos)` / `find_next(pos)`) и Scala (`iteratorFrom(start)`). Swift предоставляет generic collection navigation по opaque `Index` (`index(after:)` / `index(before:)`), что не является аналогом lower-bound API. Range projection реализуется четырьмя подходами: eager-copy (Java `get(fromIndex, toIndex)` → new `BitSet`), slice-based (`bitvec` `[range]` → `&BitSlice`), SortedSet-based (Scala `range`/`from`/`until` → новый `BitSet`), collection subscript (Swift `subscript(members:)` → `Slice<BitSet>`). Chunks/windows — специфика slice-модели `bitvec`, не характерная для set-oriented дизайнов. Отдельная ось — range mutation / range predicates: `fixedbitset` (`insert_range`/`remove_range`/`toggle_range`/`set_range`, `contains_all_in_range`/`contains_any_in_range`), Boost (`set`/`reset`/`flip` с `(pos, len)`), Java (`set`/`clear`/`flip` с `(from, to)`) — range-мутации без projection.

Для Kotlin: `Iterable<Int>` / `Iterator<Int>` по индексам set-битов — основной паттерн. Дополнительно: `forEachBit {}` (inline, высокопроизводительный), `asSequence()`. Start-from навигация (`nextSetBit(fromIndex)`) — обязательный минимум. Вопрос range projection (view vs copy, lazy vs eager) → шаг 8.

---

## 10. Открытые вопросы для последующих шагов

1. **`Set<Int>` vs отдельная иерархия** — реализация `Set<Int>` обеспечивает максимальную интеграцию (Scala), но привносит ограничения (`equals` совместимость с `Set.equals`, boxing при итерации?). Swift выбрал `SetAlgebra` (отдельный протокол), не `Set`. Решение → шаг 8.

2. **Naming: `BitSet` vs `BitArray` vs другое** — Swift разделяет `BitSet` (множество) и `BitArray` (вектор). Для Kotlin имя `BitSet` привычно из Java. Если тип моделирует множество — `BitSet` уместно. Если вектор — `BitArray` точнее. Решение → шаг 8.

3. **`IntArray` vs `LongArray` на JS** — BitVector (из шага 1) использует `IntArray` из-за Long-эмуляции на JS. Swift использует platform-native `UInt`. Решение → шаг 10.

4. **Small-set optimization** — Scala immutable BitSet хранит ≤128 бит в inline `Long`-полях без аллокации массива (observed impl detail, 2.13.18; via deprecated internal classes). Для Kotlin: inline value class для single-word? Решение → шаг 8/10.

5. **Optional count caching** — Swift `BitSet.Counted` (обёртка над `BitSet`, кеширующая `count` для O(1) доступа к количеству set-битов и `isEmpty`). Это отдельная идея от small-set optimization: `Counted` не оптимизирует хранение маленьких множеств, а устраняет O(n) пересчёт popcount при частых запросах `count`. Trade-off: дополнительный overhead на мутациях. Решение → шаг 8.

6. **`*_count()` операции** (fixedbitset) — `union_count()`, `intersection_count()` без аллокации. Полезно для graph algorithms. Стоит ли включать в первую версию API? → шаг 8.

7. **Отдельный `BitArray` тип?** — Swift явно разделяет use cases. Для Kotlin: один тип `BitSet` или два (`BitSet` + `BitArray`)? → шаг 8.
