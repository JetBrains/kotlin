# Шаг 2 (частичный). Rust: `bitvec` и `fixedbitset` — детальный API surface

## Резюме

Проанализированы две основные Rust-реализации bit-коллекций: `bitvec` v1.0.1 (полнофункциональная библиотека с семейством типов, построенным по аналогии с `Vec`/`[T]`/`Box<[T]>` для bit-уровневого обращения к памяти) и `fixedbitset` v0.5.7 (прагматичная SIMD-оптимизированная реализация множества целых чисел). `bitvec` предлагает богатую систему типов с generic-параметрами для порядка бит и типа хранения, полную интеграцию с Rust ownership/borrowing моделью и Deref-цепочку `BitVec -> BitSlice` (аналог `Vec<T> -> [T]`). `fixedbitset` — минималистичная структура без generics, ориентированная на производительность (SIMD на SSE2/AVX/AVX2/Wasm), с API в стиле множеств (`insert`/`remove`/`contains`/`union_with`). Обе поддерживают итерацию по установленным битам, но фундаментально различаются в абстракции: `bitvec` моделирует *последовательность бит* (аналог `Vec<bool>`), а `fixedbitset` моделирует *множество целых чисел*.

---

## 1. `bitvec` crate (v1.0.1)

**Репозиторий:** https://github.com/bitvecto-rs/bitvec
**Лицензия:** MIT
**Зависимости:** `funty`, `radium`, `tap`, `wyz` (normal); `serde` (optional)
**`no_std`:** да (с `alloc` для `BitVec`/`BitBox`)

### 1.1. Архитектура типов

`bitvec` повторяет иерархию стандартных Rust-контейнеров для побитового доступа:

| Rust std | bitvec аналог | Ownership | Размер |
|---|---|---|---|
| `Vec<T>` | `BitVec<T, O>` | Owned, heap, growable | Динамический |
| `[T]` | `BitSlice<T, O>` | Borrowed (unsized) | Фиксированный (вид) |
| `Box<[T]>` | `BitBox<T, O>` | Owned, heap, fixed | Фиксированный |
| `[T; N]` | `BitArray<A, O>` | Owned, stack, fixed | Фиксированный (compile-time) |

Deref-цепочка: `BitVec<T,O>` -> `BitSlice<T,O>` (через `Deref`/`DerefMut`), `BitBox<T,O>` -> `BitSlice<T,O>`.

### 1.2. Generic-параметры

Все типы параметризованы двумя generic-ами:

| Параметр | Трейт-ограничение | Описание | Значение по умолчанию |
|---|---|---|---|
| `T` | `BitStore` | Тип элемента хранения | `usize` |
| `O` | `BitOrder` | Порядок нумерации бит внутри элемента | `Lsb0` |

**`BitStore` реализован для:** `u8`, `u16`, `u32`, `u64`, `usize` и их `Cell<_>`, `AtomicU*` варианты.

**`BitOrder` реализации:**
- `Lsb0` — бит 0 = младший бит элемента (LSB first)
- `Msb0` — бит 0 = старший бит элемента (MSB first, как в сетевом порядке)

### 1.3. `BitVec<T: BitStore, O: BitOrder>` — динамический битовый вектор

Аналог `Vec<bool>`, но с побитовым хранением. Владеет heap-аллокацией, поддерживает рост.

#### Конструкторы

| Сигнатура | Описание |
|---|---|
| `BitVec::new() -> Self` | Пустой вектор (без аллокации) |
| `BitVec::with_capacity(capacity: usize) -> Self` | Пустой вектор с pre-allocated capacity в битах |
| `unsafe BitVec::from_raw_parts(bitptr: BitPtr<Mut, T, O>, length: usize, capacity: usize) -> Self` | Из сырых частей (аналог `Vec::from_raw_parts`) |
| `BitVec::from_element(elem: T) -> Self` | Из одного элемента хранения |
| `BitVec::from_slice(slice: &[T]) -> Self` | Копия из среза элементов хранения |
| `BitVec::from_vec(vec: Vec<T>) -> Self` | Move из `Vec<T>` (zero-copy) |
| `BitVec::from_bitslice(slice: &BitSlice<T, O>) -> Self` | Копия из `BitSlice` |
| `BitVec::repeat(bit: bool, len: usize) -> Self` | Заполнение значением |
| `bitvec!` macro | Литерал: `bitvec![0, 1, 1, 0]` или `bitvec![1; 100]` |

#### Capacity management

| Сигнатура | Описание |
|---|---|
| `fn capacity(&self) -> usize` | Текущая ёмкость в битах |
| `fn reserve(&mut self, additional: usize)` | Зарезервировать место для минимум `additional` бит |
| `fn reserve_exact(&mut self, additional: usize)` | Минимальный резерв (без запаса) |
| `fn shrink_to_fit(&mut self)` | Освободить избыточную ёмкость |
| `fn into_raw_parts(self) -> (BitPtr<Mut, T, O>, usize, usize)` | Деструктурировать в части (disarms destructor) |

#### Мутация (push/pop/insert/remove)

| Сигнатура | Описание |
|---|---|
| `fn push(&mut self, value: bool)` | Добавить бит в конец |
| `fn pop(&mut self) -> Option<bool>` | Убрать и вернуть последний бит |
| `fn insert(&mut self, index: usize, value: bool)` | Вставить бит по индексу (сдвиг вправо) |
| `fn remove(&mut self, index: usize) -> bool` | Удалить бит по индексу (сдвиг влево) |
| `fn swap_remove(&mut self, index: usize) -> bool` | Удалить бит, заполнив последним (O(1)) |
| `fn truncate(&mut self, new_len: usize)` | Укоротить до `new_len` |
| `fn resize(&mut self, new_len: usize, value: bool)` | Изменить длину, заполняя `value` |
| `fn resize_with<F: FnMut() -> bool>(&mut self, new_len: usize, f: F)` | Изменить длину с генератором |
| `fn clear(&mut self)` | Обнулить длину (не освобождает память) |
| `fn retain<F: FnMut(usize, &bool) -> bool>(&mut self, f: F)` | Оставить только биты, удовлетворяющие предикату |
| `fn append(&mut self, other: &mut Self)` | Переместить биты из `other` в конец |
| `fn split_off(&mut self, at: usize) -> Self` | Отщепить хвост начиная с `at` |
| `fn extend_from_bitslice(&mut self, other: &BitSlice<T, O>)` | Добавить из `BitSlice` |
| `fn extend_from_raw_slice(&mut self, other: &[T])` | Добавить из `[T]` |
| `unsafe fn set_len(&mut self, new_len: usize)` | Установить длину без инициализации |
| `fn drain<R: RangeBounds<usize>>(&mut self, range: R) -> Drain<'_, T, O>` | Drain-итератор по диапазону |
| `fn splice<R, I>(&mut self, range: R, replace_with: I) -> Splice<'_, T, O, I::IntoIter>` | Замена диапазона |

#### Конверсии

| Сигнатура | Описание |
|---|---|
| `fn into_vec(self) -> Vec<T>` | В `Vec<T>` (zero-copy, передача владения) |
| `fn into_boxed_bitslice(self) -> BitBox<T, O>` | В `BitBox` (shrink + передача владения) |
| `fn as_bitslice(&self) -> &BitSlice<T, O>` | Borrow как `&BitSlice` |
| `fn as_mut_bitslice(&mut self) -> &mut BitSlice<T, O>` | Mutable borrow как `&mut BitSlice` |
| `fn as_raw_slice(&self) -> &[T]` | Доступ к сырым элементам хранения |
| `fn as_raw_mut_slice(&mut self) -> &mut [T]` | Мутабельный доступ к сырым элементам |
| `fn as_bitptr(&self) -> BitPtr<Const, T, O>` | Получить bit-pointer |
| `fn as_mut_bitptr(&mut self) -> BitPtr<Mut, T, O>` | Мутабельный bit-pointer |
| `fn force_align(&mut self)` | Выровнять начало по границе элемента |
| `fn set_uninitialized(&mut self, value: bool)` | Установить значение для битов за пределами len |

### 1.4. `BitSlice<T: BitStore, O: BitOrder>` — borrowed bit slice

Unsized тип (аналог `[T]`/`str`). Не может существовать по значению, только за ссылкой (`&BitSlice`, `&mut BitSlice`).

#### Конструкторы

| Сигнатура | Описание |
|---|---|
| `BitSlice::from_element(elem: &T) -> &Self` | Вид на один элемент |
| `BitSlice::from_element_mut(elem: &mut T) -> &mut Self` | Мутабельный вид на один элемент |
| `BitSlice::from_slice(slice: &[T]) -> &Self` | Вид на срез элементов |
| `BitSlice::from_slice_mut(slice: &mut [T]) -> &mut Self` | Мутабельный вид на срез |
| `bits!` macro | Литерал для `&BitSlice`: `bits![0, 1, 1]` |

#### Queries (read-only)

| Сигнатура | Описание |
|---|---|
| `fn len(&self) -> usize` | Длина в битах |
| `fn is_empty(&self) -> bool` | Пустой ли |
| `fn first(&self) -> Option<&bool>` (proxy) | Первый бит |
| `fn last(&self) -> Option<&bool>` (proxy) | Последний бит |
| `fn get<I: BitSliceIndex>(index: I) -> Option<...>` | Безопасное индексирование |
| `fn count_ones(&self) -> usize` | Количество установленных бит (popcount) |
| `fn count_zeros(&self) -> usize` | Количество нулевых бит |
| `fn any(&self) -> bool` | Есть ли хоть один set-бит |
| `fn all(&self) -> bool` | Все ли биты установлены |
| `fn not_any(&self) -> bool` | Нет ни одного set-бита |
| `fn not_all(&self) -> bool` | Не все биты установлены |
| `fn some(&self) -> bool` | Alias для `any` |
| `fn first_one(&self) -> Option<usize>` | Индекс первого set-бита |
| `fn first_zero(&self) -> Option<usize>` | Индекс первого clear-бита |
| `fn last_one(&self) -> Option<usize>` | Индекс последнего set-бита |
| `fn last_zero(&self) -> Option<usize>` | Индекс последнего clear-бита |
| `fn leading_ones(&self) -> usize` | Количество ведущих единиц |
| `fn leading_zeros(&self) -> usize` | Количество ведущих нулей |
| `fn trailing_ones(&self) -> usize` | Количество завершающих единиц |
| `fn trailing_zeros(&self) -> usize` | Количество завершающих нулей |
| `fn starts_with<T2, O2>(&self, prefix: &BitSlice<T2, O2>) -> bool` | Проверка префикса |
| `fn ends_with<T2, O2>(&self, suffix: &BitSlice<T2, O2>) -> bool` | Проверка суффикса |
| `fn contains<T2, O2>(&self, needle: &BitSlice<T2, O2>) -> bool` | Поиск подпоследовательности |

#### Мутация (на месте)

| Сигнатура | Описание |
|---|---|
| `fn set(&mut self, index: usize, value: bool)` | Установить бит по индексу |
| `unsafe fn set_unchecked(&mut self, index: usize, value: bool)` | Без проверки границ |
| `fn fill(&mut self, value: bool)` | Заполнить все биты |
| `fn swap(&mut self, a: usize, b: usize)` | Поменять биты местами |
| `fn reverse(&mut self)` | Обратить порядок бит |
| `fn copy_within<R: RangeBounds<usize>>(&mut self, src: R, dest: usize)` | Копировать внутри slice |
| `fn copy_from_bitslice(&mut self, src: &Self)` | Копировать из другого BitSlice |
| `fn shift_left(&mut self, by: usize)` | Сдвиг бит влево (заполнение нулями) |
| `fn shift_right(&mut self, by: usize)` | Сдвиг бит вправо (заполнение нулями) |

#### Splitting / sub-slicing

| Сигнатура | Описание |
|---|---|
| `fn split_at(&self, mid: usize) -> (&Self, &Self)` | Разделить на два slice |
| `fn split_at_mut(&mut self, mid: usize) -> (&mut Self, &mut Self)` | Мутабельное разделение |
| `fn split_first(&self) -> Option<(&bool, &Self)>` (proxy) | Первый бит + остаток |
| `fn split_last(&self) -> Option<(&bool, &Self)>` (proxy) | Последний бит + начало |
| `fn chunks(&self, chunk_size: usize) -> Chunks<'_, T, O>` | Итератор по chunk-ам |
| `fn chunks_mut(&mut self, chunk_size: usize) -> ChunksMut<'_, T, O>` | Мутабельные chunk-и |
| `fn chunks_exact(&self, chunk_size: usize) -> ChunksExact<'_, T, O>` | Chunk-и точного размера |
| `fn chunks_exact_mut(&mut self, chunk_size: usize) -> ChunksExactMut<'_, T, O>` | Мутабельные точные chunk-и |
| `fn rchunks(&self, chunk_size: usize) -> RChunks<'_, T, O>` | Обратные chunk-и |
| `fn rchunks_mut(&mut self, ...) -> RChunksMut<'_, T, O>` | Мутабельные обратные chunk-и |
| `fn rchunks_exact(&self, ...) -> RChunksExact<'_, T, O>` | Обратные точные chunk-и |
| `fn rchunks_exact_mut(&mut self, ...) -> RChunksExactMut<'_, T, O>` | Мутабельные обратные точные chunk-и |
| `fn windows(&self, size: usize) -> Windows<'_, T, O>` | Скользящее окно |
| `fn split<F>(&self, pred: F) -> Split<'_, T, O, F>` | Разбиение по предикату |
| `fn split_mut<F>(&mut self, pred: F) -> SplitMut<'_, T, O, F>` | Мутабельное разбиение |
| `fn rsplit<F>(&self, pred: F) -> RSplit<'_, T, O, F>` | Обратное разбиение |
| `fn splitn<F>(&self, n: usize, pred: F) -> SplitN<'_, T, O, F>` | Ограниченное разбиение |

#### Iteration

| Сигнатура | Описание |
|---|---|
| `fn iter(&self) -> Iter<'_, T, O>` | Итератор по `&bool` (proxy refs) |
| `fn iter_mut(&mut self) -> IterMut<'_, T, O>` | Мутабельный итератор (proxy refs) |
| `fn iter_ones(&self) -> IterOnes<'_, T, O>` | Итератор по индексам set-бит |
| `fn iter_zeros(&self) -> IterZeros<'_, T, O>` | Итератор по индексам clear-бит |

> **Ключевой момент:** `iter_ones()` и `iter_zeros()` возвращают `usize` (индексы), а `iter()` возвращает proxy-ссылки на `bool`.

#### Raw access

| Сигнатура | Описание |
|---|---|
| `fn as_raw_slice(&self) -> &[T]` | Сырые элементы хранения |
| `fn as_raw_mut_slice(&mut self) -> &mut [T]` | Мутабельные сырые элементы |
| `fn domain(&self) -> Domain<'_, Const, T, O>` | Typed domain decomposition |
| `fn domain_mut(&mut self) -> Domain<'_, Mut, T, O>` | Мутабельная domain decomposition |

### 1.5. `BitArray<A = [usize; 1], O: BitOrder = Lsb0>` — fixed-size stack-allocated

Аналог `[bool; N]`, но с побитовым хранением. Размер определяется типом `A` (массив store-элементов).

| Сигнатура | Описание |
|---|---|
| `BitArray::new(a: A) -> Self` | Из значения хранения |
| `BitArray::ZERO -> Self` | Все биты нулевые (const) |
| `fn as_bitslice(&self) -> &BitSlice<A::Store, O>` | Borrow как BitSlice |
| `fn as_mut_bitslice(&mut self) -> &mut BitSlice<A::Store, O>` | Mutable borrow |
| `fn as_raw_slice(&self) -> &[A::Store]` | Сырые элементы |
| `fn as_raw_mut_slice(&mut self) -> &mut [A::Store]` | Мутабельные сырые элементы |
| `fn into_inner(self) -> A` | Извлечь внутренний массив |
| `fn len(&self) -> usize` | Длина в битах (фиксирована compile-time) |
| `bitarr!` macro | Литерал: `bitarr![0, 1, 1, 0]` или `bitarr![u8, Msb0; 1, 0, 1]` |

> Все методы `BitSlice` доступны через `Deref<Target = BitSlice<A::Store, O>>`.

### 1.6. `BitBox<T: BitStore, O: BitOrder>` — heap-allocated fixed-size

Аналог `Box<[bool]>`, но побитово. Фиксированный размер после создания, heap-allocated.

#### Конструкторы

| Сигнатура | Описание |
|---|---|
| `BitBox::from_bitslice(slice: &BitSlice<T, O>) -> Self` | Копия из BitSlice |
| `BitBox::from_boxed_slice(boxed: Box<[T]>) -> Self` | Move из Box<[T]> |
| `BitBox::try_from_boxed_slice(boxed: Box<[T]>) -> Result<Self, Box<[T]>>` | Fallible конверсия |

#### Конверсии

| Сигнатура | Описание |
|---|---|
| `fn into_bitvec(self) -> BitVec<T, O>` | В BitVec (zero-copy) |
| `fn into_boxed_slice(self) -> Box<[T]>` | В Box<[T]> (zero-copy) |
| `fn into_vec(self) -> Vec<T>` | В Vec<T> |
| `fn as_bitslice(&self) -> &BitSlice<T, O>` | Borrow |
| `fn as_mut_bitslice(&mut self) -> &mut BitSlice<T, O>` | Mutable borrow |
| `fn as_raw_slice(&self) -> &[T]` | Сырые элементы |
| `fn as_raw_mut_slice(&mut self) -> &mut [T]` | Мутабельные сырые элементы |
| `fn set_uninitialized(&mut self, value: bool)` | Установить биты за пределами valid region |
| `fn force_align(&mut self)` | Выровнять начало |
| `fn bitptr(&self) -> BitPtr<Const, T, O>` | Bit-pointer |

> Все методы `BitSlice` доступны через `Deref<Target = BitSlice<T, O>>`.

### 1.7. Trait implementations (bitvec)

#### BitVec

| Trait | Описание |
|---|---|
| `Deref<Target = BitSlice<T, O>>` | Auto-deref к BitSlice |
| `DerefMut` | Мутабельный auto-deref |
| `Clone` | Глубокое копирование |
| `Default` | Пустой вектор |
| `Drop` | Освобождение памяти |
| `Debug`, `Display`, `Binary`, `Octal`, `LowerHex`, `UpperHex` | Форматирование |
| `PartialEq<BitSlice<T2, O2>>`, `PartialEq<BitVec<T2, O2>>` | Сравнение (кросс-параметрическое) |
| `PartialOrd<BitSlice<T2, O2>>` | Порядок |
| `Eq`, `Ord` | Полное равенство и порядок |
| `Hash` | Хеширование |
| `IntoIterator` (for `BitVec`, `&BitVec`, `&mut BitVec`) | Итерация (owned, borrowed, mut borrowed) |
| `FromIterator<bool>` | Сборка из итератора bool |
| `FromIterator<&bool>` | Сборка из итератора ссылок |
| `Extend<bool>`, `Extend<&bool>` | Расширение |
| `BitAnd<...>`, `BitAndAssign<...>` | Побитовое И |
| `BitOr<...>`, `BitOrAssign<...>` | Побитовое ИЛИ |
| `BitXor<...>`, `BitXorAssign<...>` | Побитовое XOR |
| `Not` | Побитовое отрицание |
| `Index<usize>` -> `&bool` (proxy) | `bv[i]` для чтения |
| `Shl<usize>`, `ShlAssign<usize>` | Сдвиг влево (bit-level) |
| `Shr<usize>`, `ShrAssign<usize>` | Сдвиг вправо (bit-level) |
| `From<&BitSlice<T, O>>` | Из BitSlice |
| `From<BitArray<A, O>>` | Из BitArray |
| `From<BitBox<T, O>>` | Из BitBox |
| `From<Vec<T>>` | Из Vec<T> |
| `From<&[T]>` | Из slice элементов |
| `TryFrom<&BitSlice<T, O>>` | Fallible конверсия |
| `Serialize` / `Deserialize` (feature `serde`) | Serde support |
| `Write` (`std::io`) | Write бит как байты |

#### BitSlice

| Trait | Описание |
|---|---|
| `PartialEq`, `PartialOrd`, `Eq`, `Ord` | Кросс-параметрическое сравнение |
| `Hash` | Хеширование |
| `Debug`, `Display`, `Binary`, `Octal`, `LowerHex`, `UpperHex` | Форматирование |
| `Index<usize>` -> `&bool` (proxy) | Индексирование |
| `Index<Range*>` -> `&BitSlice` | Sub-slicing через `[]` |
| `BitAnd`, `BitOr`, `BitXor`, `Not` (возвращают `BitVec`) | Побитовые операции (аллоцируют результат) |
| `IntoIterator` (для `&BitSlice`, `&mut BitSlice`) | Итерация |
| `ToOwned<Owned = BitVec<T, O>>` | `.to_owned()` -> `BitVec` |
| `Serialize` / `Deserialize` (feature `serde`) | Serde support |

#### BitArray

| Trait | Описание |
|---|---|
| `Deref<Target = BitSlice<A::Store, O>>` | Auto-deref |
| `DerefMut` | Мутабельный auto-deref |
| `Clone`, `Copy` | **Copy-семантика** (stack-allocated) |
| `Default` | Нулевой массив |
| `PartialEq`, `Eq`, `PartialOrd`, `Ord`, `Hash` | Полное сравнение |
| `Debug`, `Display`, `Binary`, `Octal`, `LowerHex`, `UpperHex` | Форматирование |
| `Index<usize>` -> `&bool`, `Index<Range*>` -> `&BitSlice` | Индексирование |
| `BitAnd`, `BitOr`, `BitXor`, `Not` | Побитовые (возвращают `BitArray`) |
| `BitAndAssign`, `BitOrAssign`, `BitXorAssign` | In-place побитовые |
| `IntoIterator` (owned, ref, mut ref) | Итерация |
| `From<A>` | Из массива |
| `Serialize` / `Deserialize` (feature `serde`) | Serde support |

#### BitBox

| Trait | Описание |
|---|---|
| `Deref<Target = BitSlice<T, O>>` | Auto-deref |
| `DerefMut` | Мутабельный auto-deref |
| `Clone` | Глубокое копирование |
| `Drop` | Освобождение |
| `Default` | Пустой box |
| `PartialEq`, `Eq`, `PartialOrd`, `Ord`, `Hash` | Сравнение |
| `Debug`, `Display`, `Binary`, `Octal`, `LowerHex`, `UpperHex` | Форматирование |
| `BitAnd`, `BitOr`, `BitXor`, `Not` | Побитовые |
| `IntoIterator` (owned, ref, mut ref) | Итерация |
| `From<&BitSlice>`, `From<BitVec>`, `From<Box<[T]>>` | Конверсии |
| `TryFrom<Box<[T]>>` | Fallible конверсия |
| `Serialize` / `Deserialize` (feature `serde`) | Serde support |

### 1.8. Макросы

| Макрос | Результат | Пример |
|---|---|---|
| `bitvec![...]` | `BitVec<_, _>` | `bitvec![u8, Msb0; 0, 1, 1, 0]` или `bitvec![1; 64]` |
| `bits![...]` | `&BitSlice<_, _>` | `bits![0, 1, 1]` |
| `bitarr![...]` | `BitArray<_, _>` | `bitarr![u32, Lsb0; 0, 1, 0, 1]` |
| `bitbox![...]` | `BitBox<_, _>` | `bitbox![0, 1, 1, 0]` |

---

## 2. `fixedbitset` crate (v0.5.7)

**Репозиторий:** https://github.com/petgraph/fixedbitset
**Лицензия:** MIT OR Apache-2.0
**Зависимости:** `serde` (optional)
**`no_std`:** да (с `alloc`)
**SIMD:** автоматическая оптимизация для SSE2/AVX/AVX2 (x86/x86_64) и wasm32 SIMD

### 2.1. Struct

```rust
#[derive(Debug, Eq)]
pub struct FixedBitSet {
    data: NonNull<MaybeUninit<SimdBlock>>,
    capacity: usize,
    length: usize, // в битах
}
```

Простая структура без generic-параметров. Внутреннее представление — `usize`-блоки (упакованные в SIMD-блоки при доступности). `pub type Block = usize`.

### 2.2. Конструкторы

| Сигнатура | Описание |
|---|---|
| `FixedBitSet::new() -> Self` | Пустой (const, без аллокации) |
| `FixedBitSet::with_capacity(bits: usize) -> Self` | С указанным количеством бит, все clear |
| `FixedBitSet::with_capacity_and_blocks(bits: usize, blocks: impl IntoIterator<Item = Block>) -> Self` | С ёмкостью и начальными данными из блоков |

### 2.3. Capacity и размер

| Сигнатура | Описание |
|---|---|
| `fn len(&self) -> usize` | Длина в битах (включая set и unset) |
| `fn is_empty(&self) -> bool` | `true` если длина 0 |
| `fn grow(&mut self, bits: usize)` | Увеличить до `bits` бит (только рост, не уменьшение) |
| `fn grow_and_insert(&mut self, bit: usize)` | Grow + insert одной операцией |

### 2.4. Queries (чтение)

| Сигнатура | Описание |
|---|---|
| `fn contains(&self, bit: usize) -> bool` | Установлен ли бит (out-of-bounds -> `false`) |
| `unsafe fn contains_unchecked(&self, bit: usize) -> bool` | Без проверки границ |
| `fn is_clear(&self) -> bool` | Все биты нулевые |
| `fn is_full(&self) -> bool` | Все биты установлены |
| `fn minimum(&self) -> Option<usize>` | Индекс наименьшего set-бита |
| `fn maximum(&self) -> Option<usize>` | Индекс наибольшего set-бита |
| `fn count_ones(&self, range: impl IndexRange) -> usize` | Количество set-бит в диапазоне |
| `fn count_zeroes(&self, range: impl IndexRange) -> usize` | Количество clear-бит в диапазоне |
| `fn is_disjoint(&self, other: &Self) -> bool` | Нет общих set-бит |
| `fn is_subset(&self, other: &Self) -> bool` | Подмножество |
| `fn is_superset(&self, other: &Self) -> bool` | Надмножество |
| `fn contains_all_in_range(&self, range: impl IndexRange) -> bool` | Все биты в диапазоне установлены |
| `fn contains_any_in_range(&self, range: impl IndexRange) -> bool` | Хотя бы один бит в диапазоне установлен |

### 2.5. Мутация (одиночные биты)

| Сигнатура | Описание |
|---|---|
| `fn insert(&mut self, bit: usize)` | Установить бит (panics если out of bounds) |
| `unsafe fn insert_unchecked(&mut self, bit: usize)` | Без проверки |
| `fn remove(&mut self, bit: usize)` | Очистить бит (panics если out of bounds) |
| `unsafe fn remove_unchecked(&mut self, bit: usize)` | Без проверки |
| `fn put(&mut self, bit: usize) -> bool` | Insert, вернуть предыдущее значение |
| `unsafe fn put_unchecked(&mut self, bit: usize) -> bool` | Без проверки |
| `fn toggle(&mut self, bit: usize)` | Инвертировать бит |
| `unsafe fn toggle_unchecked(&mut self, bit: usize)` | Без проверки |
| `fn set(&mut self, bit: usize, enabled: bool)` | Установить в заданное значение |
| `unsafe fn set_unchecked(&mut self, bit: usize, enabled: bool)` | Без проверки |
| `fn copy_bit(&mut self, from: usize, to: usize)` | Скопировать значение бита |
| `unsafe fn copy_bit_unchecked(&mut self, from: usize, to: usize)` | Без проверки |

### 2.6. Мутация (диапазоны)

| Сигнатура | Описание |
|---|---|
| `fn insert_range(&mut self, range: impl IndexRange)` | Установить все биты в диапазоне |
| `fn remove_range(&mut self, range: impl IndexRange)` | Очистить все биты в диапазоне |
| `fn toggle_range(&mut self, range: impl IndexRange)` | Инвертировать все биты в диапазоне |
| `fn set_range(&mut self, range: impl IndexRange, enabled: bool)` | Установить диапазон в значение |
| `fn clear(&mut self)` | Очистить все биты |

### 2.7. Bulk set operations (in-place)

Все `_with` методы модифицируют `self` на месте. Если `other` короче, недостающие биты трактуются как 0.

| Сигнатура | Описание |
|---|---|
| `fn union_with(&mut self, other: &Self)` | `self \|= other` |
| `fn intersect_with(&mut self, other: &Self)` | `self &= other` |
| `fn difference_with(&mut self, other: &Self)` | `self &= !other` |
| `fn symmetric_difference_with(&mut self, other: &Self)` | `self ^= other` |
| `fn union_count(&self, other: &Self) -> usize` | `\|self \| other\|.count_ones()` без аллокации |
| `fn intersection_count(&self, other: &Self) -> usize` | `\|self & other\|.count_ones()` |
| `fn difference_count(&self, other: &Self) -> usize` | `\|self & !other\|.count_ones()` |
| `fn symmetric_difference_count(&self, other: &Self) -> usize` | `\|self ^ other\|.count_ones()` |

### 2.8. Iteration

| Сигнатура | Описание | Возвращает |
|---|---|---|
| `fn ones(&self) -> Ones<'_>` | Set-биты (by ref) | `usize` (индексы) |
| `fn zeroes(&self) -> Zeroes<'_>` | Clear-биты (by ref) | `usize` (индексы) |
| `fn into_ones(self) -> IntoOnes` | Set-биты (consuming) | `usize` (индексы) |
| Нет `into_zeroes` | — | — |

Все итераторы реализуют `Iterator`, `DoubleEndedIterator` (для `Ones`), `FusedIterator`.

`IntoOnes` реализует `Iterator`, `FusedIterator` (consuming ownership — полезно чтобы избежать borrow).

### 2.9. Raw access

| Сигнатура | Описание |
|---|---|
| `fn as_slice(&self) -> &[Block]` | Сырые usize-блоки |
| `fn as_mut_slice(&mut self) -> &mut [Block]` | Мутабельные блоки |
| `unsafe fn from_raw_parts(data: NonNull<Block>, length: usize, capacity: usize) -> Self` | Из сырых частей |
| `fn into_raw_parts(self) -> (NonNull<Block>, usize, usize)` | Деструктурировать |

### 2.10. Trait implementations (fixedbitset)

| Trait | Описание |
|---|---|
| `Clone` | Глубокое копирование |
| `Default` | Пустой (эквив. `new()`) |
| `Drop` | Освобождение памяти |
| `Debug`, `Display`, `Binary` | Форматирование (Binary: `1010...`, Display: `{2, 5, 7}` в формате множества) |
| `PartialEq`, `Eq` | Сравнение (учитывает и set, и unset биты; `[0,1] != [0,1,0]`) |
| `PartialOrd`, `Ord` | Лексикографический порядок |
| `Hash` | Хеширование |
| `Index<usize>` -> `bool` | `bitset[i]` для чтения |
| `BitAnd<&Self>` -> `Self` | `&a & &b` -> новый FixedBitSet |
| `BitAndAssign<Self>`, `BitAndAssign<&Self>` | `a &= b` in-place |
| `BitOr<&Self>` -> `Self` | `&a \| &b` |
| `BitOrAssign<Self>`, `BitOrAssign<&Self>` | `a \|= b` |
| `BitXor<&Self>` -> `Self` | `&a ^ &b` |
| `BitXorAssign<Self>`, `BitXorAssign<&Self>` | `a ^= b` |
| Нет `Not` | Отсутствует (нельзя `!bitset`) |
| `FromIterator<usize>` | Из итератора индексов: `[1, 3, 5].into_iter().collect::<FixedBitSet>()` |
| `Extend<usize>` | Расширение из итератора индексов |
| `IntoIterator` (для `FixedBitSet`) -> `IntoOnes` | Consuming iteration по set-битам |
| `IntoIterator` (для `&FixedBitSet`) -> `Ones<'_>` | Borrowed iteration по set-битам |
| `Serialize` / `Deserialize` (feature `serde`) | Serde support |
| `Send`, `Sync` | Thread-safety |

---

## 3. Сравнительный анализ

### 3.1. Мутабельность и ownership

| Аспект | `bitvec` | `fixedbitset` |
|---|---|---|
| Ownership модель | Полная Rust модель: owned (`BitVec`, `BitBox`, `BitArray`) / borrowed (`&BitSlice`, `&mut BitSlice`) | Простой owned тип, `&` / `&mut` |
| Copy | Только `BitArray` (stack) | Нет (heap-allocated) |
| Clone | `BitVec`, `BitBox`, `BitArray` | Да |
| Deref-цепочка | `BitVec`/`BitBox`/`BitArray` -> `BitSlice` | Нет (нет slice-типа) |
| Interior mutability | Поддержка `Cell<T>` и `AtomicU*` как `BitStore` | Нет |

### 3.2. Размерная модель

| Аспект | `bitvec` | `fixedbitset` |
|---|---|---|
| Динамический рост | `BitVec` — полный `Vec`-подобный API | `grow()` — только рост (нет shrink) |
| Фиксированный (heap) | `BitBox` | Основная модель (но может `grow`) |
| Фиксированный (stack) | `BitArray<[u32; N]>` | Нет |
| Capacity vs Length | Разделены (как `Vec`) | `length` = количество бит; capacity управляется внутренне |
| Max size | `BitSlice::MAX_BITS` | Ограничено размером `usize` |

### 3.3. Collection interfaces

| Trait | `bitvec` | `fixedbitset` |
|---|---|---|
| `Iterator` (по значениям) | `IntoIterator` для owned / `&` / `&mut` | `IntoIterator` по set-битам (не по всем битам!) |
| `FromIterator` | `FromIterator<bool>` (собирает вектор бит) | `FromIterator<usize>` (собирает множество индексов) |
| `Extend` | `Extend<bool>` | `Extend<usize>` |
| `Index` | `Index<usize>` -> proxy `&bool`; `Index<Range>` -> `&BitSlice` | `Index<usize>` -> `bool` |
| `ToOwned` | `BitSlice -> BitVec` | Нет (нет slice-типа) |

> **Ключевое различие в семантике `IntoIterator`:**
> - `bitvec`: итерирует по **всем битам** (`true`/`false`), как `Vec<bool>`
> - `fixedbitset`: итерирует **только по set-битам**, возвращая их **индексы** (`usize`)

### 3.4. Iteration support

| Возможность | `bitvec` | `fixedbitset` |
|---|---|---|
| По всем битам (bool) | `iter()` / `IntoIterator` | Нет |
| По индексам set-бит | `iter_ones()` | `ones()` / `IntoIterator` / `into_ones()` |
| По индексам clear-бит | `iter_zeros()` | `zeroes()` |
| Consuming по set-битам | Нет dedicated | `into_ones()` |
| DoubleEndedIterator | `iter()`, `iter_ones()`, `iter_zeros()` | `ones()` |
| По chunk-ам | `chunks()`, `chunks_exact()`, `rchunks()` и др. | Нет |
| Sliding windows | `windows()` | Нет |
| Split по предикату | `split()`, `splitn()` и др. | Нет |
| Drain | `drain()` | Нет |
| Splice | `splice()` | Нет |

### 3.5. Operators и синтаксический сахар

| Оператор | `bitvec` (`BitVec`/`BitSlice`) | `fixedbitset` |
|---|---|---|
| `&` (BitAnd) | Да, возвращает `BitVec` | Да, `&a & &b` -> новый `FixedBitSet` |
| `\|` (BitOr) | Да | Да |
| `^` (BitXor) | Да | Да |
| `!` (Not) | Да (инвертирует все биты) | **Нет** |
| `&=` | Да | Да |
| `\|=` | Да | Да |
| `^=` | Да | Да |
| `<<`, `<<=` (Shl) | Да (bit-level shift) | Нет |
| `>>`, `>>=` (Shr) | Да (bit-level shift) | Нет |
| `[]` indexing | Да (proxy ref, range sub-slicing) | Да (returns `bool`) |
| `[]` range slicing | Да (`bs[2..5]` -> `&BitSlice`) | Нет |
| `==` / `!=` | Да (кросс-тип) | Да |
| `<` / `>` / `<=` / `>=` | Да | Да |

### 3.6. Serialization и interop

| Аспект | `bitvec` | `fixedbitset` |
|---|---|---|
| Serde | Feature `serde`, для всех типов | Feature `serde` |
| Конверсия в/из `Vec<T>` | `from_vec()` / `into_vec()` (zero-copy) | Нет прямой; `as_slice()` даёт `&[usize]` |
| Конверсия в/из массивов | `BitArray::from(arr)` / `into_inner()` | Нет |
| Конверсия в/из Box | `from_boxed_slice()` / `into_boxed_slice()` | Нет |
| Raw parts | `from_raw_parts()` / `into_raw_parts()` | `from_raw_parts()` / `into_raw_parts()` |
| `std::io::Write` | Да (для `BitVec`) | Нет |
| Из блоков | `from_slice(&[T])` | `with_capacity_and_blocks(bits, impl IntoIterator<Item=usize>)` |
| Display формат | Binary: `[01101010]` | Set: `{2, 5, 7}` или Binary: `01101010` |

### 3.7. Set operations

| Операция | `bitvec` | `fixedbitset` |
|---|---|---|
| Union | `\|` / `\|=` оператор | `union_with(&mut self, &Self)` + `\|` оператор |
| Intersection | `&` / `&=` оператор | `intersect_with(&mut self, &Self)` + `&` оператор |
| Difference | Нет dedicated (ручное `a & !b`) | `difference_with(&mut self, &Self)` |
| Symmetric diff | `^` / `^=` оператор | `symmetric_difference_with(&mut self, &Self)` + `^` оператор |
| Union count | Нет dedicated | `union_count(&self, &Self) -> usize` |
| Intersection count | Нет dedicated | `intersection_count(&self, &Self) -> usize` |
| Difference count | Нет dedicated | `difference_count(&self, &Self) -> usize` |
| Sym diff count | Нет dedicated | `symmetric_difference_count(&self, &Self) -> usize` |
| is_disjoint | Нет dedicated | `is_disjoint(&self, &Self) -> bool` |
| is_subset | Нет dedicated | `is_subset(&self, &Self) -> bool` |
| is_superset | Нет dedicated | `is_superset(&self, &Self) -> bool` |

> **Наблюдение:** `fixedbitset` имеет значительно более богатый набор именованных set-операций, включая `*_count` варианты, которые считают popcount результата без аллокации промежуточного BitSet. Это важная оптимизация для use cases вроде graph algorithms.

---

## 4. Ключевые выводы для дизайна Kotlin BitSet

### 4.1. Две фундаментально разные абстракции

- **bitvec** моделирует *последовательность бит* (sequence / vector). `FromIterator<bool>`, `IntoIterator` по всем битам, slice API с chunks/windows/split. Conceptually: `Vec<bool>` с побитовым хранением.
- **fixedbitset** моделирует *множество целых чисел* (set). `FromIterator<usize>`, `IntoIterator` по set-битам (возвращает индексы), set-операции (is_subset, is_disjoint), `insert`/`remove`/`contains` терминология.

Это отражает фундаментальный выбор, который предстоит сделать для Kotlin: BitSet как sequence (ближе к `List<Boolean>`) или как set (ближе к `Set<Int>`). Java `BitSet` ближе к модели set, Scala предоставляет `BitSet: Set[Int]`, Swift также моделирует `SetAlgebra`.

### 4.2. Deref/delegation pattern (bitvec)

Паттерн `BitVec -> Deref -> BitSlice` позволяет определить все read-only и slice-операции один раз на `BitSlice`, а `BitVec`, `BitBox`, `BitArray` автоматически получают их через Deref. Аналог в Kotlin: delegation через interface или extension functions на общий read-only тип.

### 4.3. Bit-order parametrization (bitvec)

Generic-параметр `O: BitOrder` (`Lsb0` / `Msb0`) уникален для bitvec и позволяет работать с данными разного формата (сетевые протоколы, файловые форматы). Это скорее низкоуровневая feature, не критичная для stdlib BitSet, но стоит иметь в виду для interop scenarios.

### 4.4. SIMD optimization (fixedbitset)

`fixedbitset` демонстрирует важность SIMD для производительности bulk-операций. Для Kotlin multiplatform прямая SIMD-оптимизация затруднена, но можно обеспечить cache-friendly layout (контигуальный массив слов) и делегировать SIMD оптимизатору JIT (JVM) или использовать intrinsics (Native).

### 4.5. `*_count` операции (fixedbitset)

Методы `union_count()`, `intersection_count()`, `difference_count()`, `symmetric_difference_count()` — сильная идея. Они позволяют вычислить popcount результата set-операции без аллокации промежуточной структуры. Для Kotlin это может быть реализовано как extension или метод.

### 4.6. `minimum()` / `maximum()` (fixedbitset)

Аналоги Java `nextSetBit(0)` / перебора в обратном порядке, но с более читаемым API. Стоит рассмотреть для Kotlin API.

### 4.7. `grow_and_insert` (fixedbitset)

Атомарная операция `grow(bit + 1); insert(bit)` — практичная оптимизация для common pattern.

### 4.8. Unchecked variants (fixedbitset)

Каждая мутирующая операция имеет `unsafe *_unchecked` вариант. В Kotlin нет `unsafe`, но при проектировании можно учесть для internal implementation или @PublishedApi inline functions.

### 4.9. Отсутствие `Not` в fixedbitset

Примечательно, что `fixedbitset` не реализует `Not` (побитовое отрицание). Это осознанный выбор: для множества неопределённого размера "инвертирование" семантически неоднозначно (инвертировать только в пределах capacity?). `bitvec` реализует `Not`, т.к. size чётко определён.

### 4.10. Display format

- `bitvec`: binary representation `[01101010]`
- `fixedbitset`: set representation `{2, 5, 7}` (Display) или binary `01101010` (Binary trait)

Для Kotlin имеет смысл поддержать оба представления: `toString()` в формате множества, и отдельный метод для binary dump.
