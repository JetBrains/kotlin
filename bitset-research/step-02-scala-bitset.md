# Scala BitSet — Cross-Language API Analysis

**Source:** Scala 2.13.x standard library (`scala.collection.BitSet`, `scala.collection.immutable.BitSet`, `scala.collection.mutable.BitSet`)

---

## 1. Architecture Overview

Scala BitSet is unique among all language implementations in that it provides **two separate classes** (immutable and mutable) sharing a **common base trait**, all fully integrated into Scala's rich collection hierarchy. This is the defining characteristic of Scala's approach.

### Hierarchy Diagram

```
Iterable[Int]
  └── Set[Int]                              // full Set contract, Function1[Int, Boolean]
        └── SortedSet[Int]                  // elements always iterated in ascending order
              └── collection.BitSet         // base trait: toBitMask, nwords, word(idx), contains, iterator, xor, ^
                    ├── immutable.BitSet     // sealed abstract class: incl, excl, +, -, updateWord returns new BitSet
                    │     ├── BitSet1        // optimized: single Long (bits 0..63)
                    │     ├── BitSet2        // optimized: two Longs (bits 0..127)
                    │     └── BitSetN        // general: Array[Long]
                    └── mutable.BitSet       // class: addOne, subtractOne, +=, -=, |=, &=, ^=, &~=, clear, update
```

### Trait/Class Linearization

**`scala.collection.immutable.BitSet`** extends:
```scala
sealed abstract class BitSet
  extends AbstractSet[Int]                                        // concrete base for Set
    with SortedSet[Int]                                           // sorted iteration
    with SortedSetOps[Int, SortedSet, BitSet]                     // sorted set operations
    with StrictOptimizedSortedSetOps[Int, SortedSet, BitSet]      // optimized map/flatMap/collect
    with collection.BitSet                                        // base BitSet trait
    with collection.BitSetOps[BitSet]                             // BitSet-specific operations
    with Serializable
```

**`scala.collection.mutable.BitSet`** extends:
```scala
class BitSet(protected[collection] final var elems: Array[Long])
  extends AbstractSet[Int]
    with SortedSet[Int]
    with SortedSetOps[Int, SortedSet, BitSet]
    with StrictOptimizedIterableOps[Int, Set, BitSet]
    with StrictOptimizedSortedSetOps[Int, SortedSet, BitSet]
    with collection.BitSet
    with collection.BitSetOps[BitSet]
    with Serializable
```

**`scala.collection.BitSet`** (base trait) extends:
```scala
trait BitSet extends SortedSet[Int] with BitSetOps[BitSet]
```

**`scala.collection.BitSetOps[+C]`** (operations trait) extends:
```scala
trait BitSetOps[+C <: BitSet with BitSetOps[C]]
  extends SortedSetOps[Int, SortedSet, C]
```

---

## 2. Factory Methods & Companion Objects

### `scala.collection.BitSet` (base companion)

| Method | Signature | Description |
|--------|-----------|-------------|
| `empty` | `def empty: BitSet` | Delegates to `immutable.BitSet.empty` |
| `newBuilder` | `def newBuilder: Builder[Int, BitSet]` | Delegates to `immutable.BitSet.newBuilder` |
| `fromSpecific` | `def fromSpecific(it: IterableOnce[Int]): BitSet` | Delegates to `immutable.BitSet.fromSpecific` |

### `scala.collection.immutable.BitSet` (companion)

| Method | Signature | Description |
|--------|-----------|-------------|
| `empty` | `val empty: BitSet` | Returns `BitSet1(0L)` -- singleton empty |
| `newBuilder` | `def newBuilder: Builder[Int, BitSet]` | Uses `mutable.BitSet.newBuilder` internally, converts result |
| `fromSpecific` | `def fromSpecific(it: IterableOnce[Int]): BitSet` | Reuses if already immutable BitSet; otherwise builds |
| `fromBitMask` | `def fromBitMask(elems: Array[Long]): BitSet` | Creates from array of longs, **copies** the array |
| `fromBitMaskNoCopy` | `def fromBitMaskNoCopy(elems: Array[Long]): BitSet` | Creates from array of longs, **no copy** (caller must not mutate) |
| `apply` | inherited from `SpecificIterableFactory`: `def apply(elems: Int*): BitSet` | `BitSet(1, 3, 5)` |

### `scala.collection.mutable.BitSet` (companion)

| Method | Signature | Description |
|--------|-----------|-------------|
| `empty` | `def empty: BitSet` | Returns `new BitSet()` |
| `newBuilder` | `def newBuilder: Builder[Int, BitSet]` | `GrowableBuilder(empty)` |
| `fromSpecific` | `def fromSpecific(it: IterableOnce[Int]): BitSet` | `Growable.from(empty, it)` |
| `fromBitMask` | `def fromBitMask(elems: Array[Long]): BitSet` | Creates from array of longs, **copies** the array |
| `fromBitMaskNoCopy` | `def fromBitMaskNoCopy(elems: Array[Long]): BitSet` | Creates from array of longs, **no copy** |
| `apply` | inherited: `def apply(elems: Int*): BitSet` | `mutable.BitSet(1, 3, 5)` |

### Constructors (mutable only)

| Constructor | Description |
|-------------|-------------|
| `new BitSet()` | Empty, default initial capacity |
| `new BitSet(initSize: Int)` | Pre-allocate for `initSize` bits |
| `new BitSet(elems: Array[Long])` | From raw word array (package-private) |

---

## 3. Core BitSet-Specific Methods (from `BitSetOps` trait -- shared by both)

### Internal Representation

| Method | Signature | Description |
|--------|-----------|-------------|
| `nwords` | `protected[collection] def nwords: Int` | Number of 64-bit words in the backing array |
| `word` | `protected[collection] def word(idx: Int): Long` | Word at index `idx`, or `0L` if out of range |
| `fromBitMaskNoCopy` | `protected[collection] def fromBitMaskNoCopy(elems: Array[Long]): C` | Create new BitSet from raw words |

### Query Operations

| Method | Signature | Description |
|--------|-----------|-------------|
| `contains` | `def contains(elem: Int): Boolean` | `0 <= elem && (word(elem >> 6) & (1L << elem)) != 0L` |
| `apply` | `final def apply(elem: Int): Boolean` | Alias for `contains` -- enables `bitSet(5)` syntax |
| `size` | `override def size: Int` | Sum of `Long.bitCount(word(i))` for all words |
| `isEmpty` | `override def isEmpty: Boolean` | True if all words are zero |
| `ordering` | `final def ordering: Ordering[Int]` | Always `Ordering.Int` (hardcoded) |
| `min` | `override def min: Int` | Optimized: `numberOfTrailingZeros` on first non-zero word |
| `max` | `override def max: Int` | Optimized: `(nwords * 64) - numberOfLeadingZeros - 1` |
| `subsetOf` | `def subsetOf(that: Set[Int]): Boolean` | Optimized for BitSet-vs-BitSet: word-by-word `(a & ~b) == 0` check |

### Iteration

| Method | Signature | Description |
|--------|-----------|-------------|
| `iterator` | `def iterator: Iterator[Int]` | Iterates set bits in ascending order via `numberOfTrailingZeros` |
| `iteratorFrom` | `def iteratorFrom(start: Int): Iterator[Int]` | Iterator starting from `start` (for `SortedSet` contract) |
| `foreach` | `override def foreach[U](f: Int => U): Unit` | Optimized word-by-word loop; skips zero words entirely |
| `stepper` | `override def stepper[S <: Stepper[_]](...): S with EfficientSplit` | Java Spliterator interop for parallel streams |

### Conversion

| Method | Signature | Description |
|--------|-----------|-------------|
| `toBitMask` | `def toBitMask: Array[Long]` | Returns a **copy** of the internal `Array[Long]` representation |

### Set Algebra (returning new BitSet of same type)

| Method | Operator Alias | Signature | Description |
|--------|---------------|-----------|-------------|
| `concat` / `union` | `++`, `\|` | `def concat(other: IterableOnce[Int]): C` | Union. Optimized for BitSet: word-by-word OR |
| `intersect` | `&` | `def intersect(other: Set[Int]): C` | Intersection. Optimized: word-by-word AND |
| `diff` | `&~` | `def diff(other: Set[Int]): C` | Difference. Optimized: word-by-word AND-NOT |
| `xor` | `^` | `def xor(other: BitSet): C` | Symmetric difference. Word-by-word XOR |
| `rangeImpl` | -- | `def rangeImpl(from: Option[Int], until: Option[Int]): C` | Subrange of bits (for `SortedSet.range/from/until`) |
| `partition` | -- | `override def partition(p: Int => Boolean): (C, C)` | Optimized: `(filter(p), this &~ filter(p))` |

### Higher-Order Operations (returning BitSet when possible)

| Method | Signature | Notes |
|--------|-----------|-------|
| `map` | `def map(f: Int => Int): C` | Returns BitSet (same type) |
| `map[B]` | `def map[B](f: Int => B)(implicit ev: Ordering[B]): SortedSet[B]` | Returns `SortedSet[B]` if `B != Int` |
| `flatMap` | `def flatMap(f: Int => IterableOnce[Int]): C` | Returns BitSet |
| `flatMap[B]` | `def flatMap[B](...)(implicit ev: Ordering[B]): SortedSet[B]` | Returns `SortedSet[B]` |
| `collect` | `def collect(pf: PartialFunction[Int, Int]): C` | Returns BitSet |
| `collect[B]` | `def collect[B](...)(implicit ev: Ordering[B]): SortedSet[B]` | Returns `SortedSet[B]` |
| `filter` | inherited | Returns BitSet via `filterImpl` (optimized word-by-word) |
| `filterNot` | inherited | Returns BitSet via `filterImpl` |
| `zip` | `def zip[B](...)(implicit ev: Ordering[(Int, B)]): SortedSet[(Int, B)]` | Cannot return BitSet (element type changes) |

---

## 4. Immutable-Specific API

The immutable variant returns a **new BitSet** for every modification.

### Element Operations

| Method | Operator | Signature | Description |
|--------|----------|-----------|-------------|
| `incl` | `+` | `def incl(elem: Int): BitSet` | Returns new BitSet with `elem` added. `require(elem >= 0)` |
| `excl` | `-` | `def excl(elem: Int): BitSet` | Returns new BitSet with `elem` removed. `require(elem >= 0)` |

### Bulk Operations (inherited from immutable.SetOps)

| Method | Operator | Signature | Description |
|--------|----------|-----------|-------------|
| `removedAll` | `--` | `def removedAll(that: IterableOnce[Int]): BitSet` | Remove all elements from `that` |

### Internal Implementation Classes

| Class | Storage | Bits Covered | Notes |
|-------|---------|--------------|-------|
| `BitSet1` | 1 `Long` field | 0..63 | Most common case; no allocation overhead |
| `BitSet2` | 2 `Long` fields | 0..127 | Still inline, no array |
| `BitSetN` | `Array[Long]` | 0..unlimited | General case |

**Structural sharing:** None. Each modification allocates a new `BitSet1`, `BitSet2`, or `BitSetN`. However, operations like `incl`/`excl` return `this` if the element was already present/absent (identity optimization).

### Optimized `diff` and `filterImpl`

Both `BitSet1`, `BitSet2`, and `BitSetN` override `diff` and `filterImpl` with specialized implementations that:
- Track the highest non-zero word index to avoid over-allocation
- Return `this` unchanged if no bits were actually modified (change-tracking optimization)
- Downgrade `BitSetN` to `BitSet1`/`BitSet2` when result fits in fewer words

---

## 5. Mutable-Specific API

The mutable variant modifies **in place** and returns `this` for chaining.

### Element Operations

| Method | Operator | Signature | Description |
|--------|----------|-----------|-------------|
| `addOne` | `+=` | `def addOne(elem: Int): this.type` | Add element in place. `require(elem >= 0)` |
| `subtractOne` | `-=` | `def subtractOne(elem: Int): this.type` | Remove element in place. `require(elem >= 0)` |
| `add` | -- | `def add(elem: Int): Boolean` | Add if absent; returns `true` if added (inherited from `mutable.SetOps`) |
| `remove` | -- | `def remove(elem: Int): Boolean` | Remove if present; returns `true` if was present |
| `update` | -- | `def update(elem: Int, included: Boolean): Unit` | `set(5) = true` adds, `set(5) = false` removes |
| `clear` | -- | `def clear(): Unit` | Zeros out all words (retains array size) |

### Bulk Mutation Operations

| Method | Operator | Signature | Description |
|--------|----------|-----------|-------------|
| `addAll` | `++=` | `override def addAll(xs: IterableOnce[Int]): this.type` | Optimized for `BitSet` (uses `\|=`), `Range` (bulk word fill), `SortedSet` (pre-allocate) |
| `subtractAll` | `--=` | `override def subtractAll(xs: IterableOnce[Int]): this.type` | Optimized for `BitSet` (uses `&~=`) |
| `filterInPlace` | -- | `override def filterInPlace(p: Int => Boolean): this.type` | Remove elements not matching predicate, in place |

### In-Place Set Algebra Operators

| Method | Operator | Signature | Description |
|--------|----------|-----------|-------------|
| union-assign | `\|=` | `def \|= (other: collection.BitSet): this.type` | In-place union (bitwise OR). Grows array if needed |
| intersect-assign | `&=` | `def &= (other: collection.BitSet): this.type` | In-place intersection (bitwise AND). No resize needed |
| xor-assign | `^=` | `def ^= (other: collection.BitSet): this.type` | In-place symmetric diff (bitwise XOR). Grows if needed |
| andNot-assign | `&~=` | `def &~= (other: collection.BitSet): this.type` | In-place difference (bitwise AND-NOT). No resize needed |

### Mutable-Only Utility

| Method | Signature | Description |
|--------|-----------|-------------|
| `clone` | `override def clone(): BitSet` | Deep copy via `Arrays.copyOf` |
| `toImmutable` | `def toImmutable: immutable.BitSet` | Convert to immutable via `immutable.BitSet.fromBitMask(elems)` |
| `ensureCapacity` | `protected final def ensureCapacity(idx: Int): Unit` | Doubles array size as needed; max `MaxSize` = `Int.MaxValue / 64 + 1` |

---

## 6. Inherited Collection API (from `Set[Int]`, `Iterable[Int]`, etc.)

Because BitSet implements `Set[Int]`, it inherits the **entire** Scala collections API. Key inherited methods include:

### From `IterableOps` / `Iterable[Int]`

| Method | Signature | Notes |
|--------|-----------|-------|
| `head` | `def head: Int` | First (smallest) element |
| `last` | `def last: Int` | Last (largest) element |
| `headOption` | `def headOption: Option[Int]` | |
| `tail` | `def tail: C` | All except first |
| `init` | `def init: C` | All except last |
| `take(n)` | `def take(n: Int): C` | First n elements |
| `drop(n)` | `def drop(n: Int): C` | Drop first n |
| `slice` | `def slice(from: Int, until: Int): C` | |
| `takeWhile` | `def takeWhile(p: Int => Boolean): C` | |
| `dropWhile` | `def dropWhile(p: Int => Boolean): C` | |
| `span` | `def span(p: Int => Boolean): (C, C)` | |
| `splitAt` | `def splitAt(n: Int): (C, C)` | |
| `foldLeft` | `def foldLeft[B](z: B)(op: (B, Int) => B): B` | |
| `foldRight` | `def foldRight[B](z: B)(op: (Int, B) => B): B` | |
| `reduce` | `def reduce(op: (Int, Int) => Int): Int` | |
| `exists` | `def exists(p: Int => Boolean): Boolean` | |
| `forall` | `def forall(p: Int => Boolean): Boolean` | |
| `count` | `def count(p: Int => Boolean): Int` | |
| `find` | `def find(p: Int => Boolean): Option[Int]` | |
| `sum` | `def sum: Int` | (via `Numeric[Int]`) |
| `product` | `def product: Int` | |
| `mkString` | `def mkString(sep: String): String` | |
| `toList` | `def toList: List[Int]` | |
| `toVector` | `def toVector: Vector[Int]` | |
| `toArray` | `def toArray: Array[Int]` | |
| `toSet` | `def toSet: Set[Int]` | |
| `to` | `def to[C](factory: Factory[Int, C]): C` | Generic conversion |
| `grouped` | `def grouped(size: Int): Iterator[C]` | |
| `sliding` | `def sliding(size: Int, step: Int): Iterator[C]` | |
| `zip` | `def zip[B](that: IterableOnce[B]): SortedSet[(Int, B)]` | |
| `zipWithIndex` | `def zipWithIndex: SortedSet[(Int, Int)]` | |
| `corresponds` | `def corresponds[B](that: IterableOnce[B])(p: (Int, B) => Boolean): Boolean` | |
| `knownSize` | `def knownSize: Int` | Potentially O(n) since it calls `size` |
| `sizeIs` | `def sizeIs: SizeCompareOps` | Lazy size comparison |

### From `SetOps`

| Method | Operator | Notes |
|--------|----------|-------|
| `contains` | | O(1) bit test |
| `apply` | `bitSet(5)` | Alias for `contains` -- BitSet acts as `Int => Boolean` |
| `subsetOf` | | Optimized word-by-word for BitSet |
| `subsets()` | | Iterator over all subsets |
| `subsets(len)` | | Iterator over subsets of given size |
| `intersect` | `&` | |
| `union` | `\|` | Alias for `concat` |
| `diff` | `&~` | |
| `concat` | `++` | |

### From `SortedSetOps`

| Method | Notes |
|--------|-------|
| `iteratorFrom(start: Int)` | Iterator from `start` upward |
| `firstKey` | Same as `head` (smallest element) |
| `lastKey` | Same as `last` (largest element) |
| `range(from, until)` | Subrange view via `rangeImpl` |
| `from(elem)` | Elements >= `elem` |
| `until(elem)` | Elements < `elem` |
| `rangeTo(to)` | Elements <= `to` |

### `Function1[Int, Boolean]` -- BitSet as a Predicate

Because `Set[A]` extends `A => Boolean`, any BitSet can be used as a function:
```scala
val bs = BitSet(1, 3, 5)
List(1, 2, 3, 4, 5).filter(bs)  // List(1, 3, 5)
if (bs(3)) println("contains 3")
```

---

## 7. Operator Summary

### Operators Available on BOTH Immutable and Mutable

| Operator | Method | Semantics | Return Type |
|----------|--------|-----------|-------------|
| `&` | `intersect` | Intersection | New BitSet |
| `\|` | `union` / `concat` | Union | New BitSet |
| `&~` | `diff` | Difference | New BitSet |
| `^` | `xor` | Symmetric difference | New BitSet |
| `++` | `concat` | Add all elements | New BitSet |

### Operators on Immutable Only

| Operator | Method | Semantics | Return Type |
|----------|--------|-----------|-------------|
| `+` | `incl` | Add one element | New BitSet |
| `-` | `excl` | Remove one element | New BitSet |
| `--` | `removedAll` | Remove multiple elements | New BitSet |

### Operators on Mutable Only

| Operator | Method | Semantics | Return Type |
|----------|--------|-----------|-------------|
| `+=` | `addOne` | Add one element in place | `this.type` |
| `-=` | `subtractOne` | Remove one element in place | `this.type` |
| `++=` | `addAll` | Add all elements in place | `this.type` |
| `--=` | `subtractAll` | Remove all elements in place | `this.type` |
| `\|=` | (direct) | In-place union | `this.type` |
| `&=` | (direct) | In-place intersection | `this.type` |
| `^=` | (direct) | In-place symmetric diff | `this.type` |
| `&~=` | (direct) | In-place difference | `this.type` |

---

## 8. Internal Representation & Size Model

### Storage Layout

Both variants use `Array[Long]` as the backing store, where each `Long` word holds 64 bits.

| Property | Immutable | Mutable |
|----------|-----------|---------|
| Backing storage | `Long` field(s) or `Array[Long]` | `var elems: Array[Long]` |
| Growth strategy | N/A (new object per operation) | Doubles array size; max `MaxSize = Int.MaxValue / 64 + 1` |
| Shrinking | Automatic: result uses smallest class (`BitSet1`/`2`/`N`) | Never shrinks; `clear()` zeros but keeps array |
| Small-set optimization | `BitSet1` (1 Long, no array), `BitSet2` (2 Longs, no array) | No; always `Array[Long]` (minimum 1 element) |
| Element constraint | `require(elem >= 0)` | `require(elem >= 0)` |
| Max element | Limited by memory/`Int.MaxValue` | Limited by `MaxSize * 64` |

### Word-Level Constants

```scala
LogWL     = 6          // log2(64)
WordLength = 64        // bits per Long
MaxSize   = Int.MaxValue / 64 + 1  // max number of words (~33.5 million)
```

---

## 9. Serialization & Interop

| Feature | Mechanism |
|---------|-----------|
| Java Serialization | Both implement `Serializable` with custom `SerializationProxy` |
| Serialization format | Writes `nwords: Int` followed by each `Long` word |
| `toBitMask` | Export to `Array[Long]` (always copies) |
| `fromBitMask` | Import from `Array[Long]` (copies array) |
| `fromBitMaskNoCopy` | Import from `Array[Long]` (zero-copy; caller must not mutate) |
| `toImmutable` | Mutable-only: `def toImmutable: immutable.BitSet` |
| Collection conversions | `toList`, `toSet`, `toArray`, `toVector`, `to(factory)`, etc. (all inherited) |
| Java stream interop | `stepper` provides `IntStepper with EfficientSplit` for parallel streams |
| String representation | `BitSet(1, 3, 5)` (inherited `toString` from `Iterable`) |

---

## 10. Key Design Patterns

### Pattern 1: Mutability Split via Separate Classes

The immutable/mutable split is Scala's most distinctive design choice. The same operations have different semantics:

```scala
// Immutable: returns new BitSet
val a = immutable.BitSet(1, 2, 3)
val b = a + 4        // b = BitSet(1, 2, 3, 4), a unchanged
val c = a | b        // c = BitSet(1, 2, 3, 4)

// Mutable: modifies in place
val m = mutable.BitSet(1, 2, 3)
m += 4               // m is now BitSet(1, 2, 3, 4)
m |= BitSet(5, 6)    // m is now BitSet(1, 2, 3, 4, 5, 6)
```

### Pattern 2: BitSet as Set[Int]

BitSet is a full `Set[Int]`, meaning it participates in the collection type hierarchy and can be used anywhere a `Set[Int]` is expected:

```scala
def process(s: Set[Int]): Unit = ...
process(BitSet(1, 2, 3))  // works

val s: SortedSet[Int] = BitSet(1, 2, 3)  // upcast
```

### Pattern 3: BitSet as Predicate (Function1[Int, Boolean])

```scala
val filter = BitSet(2, 4, 6, 8, 10)
(1 to 10).filter(filter)  // Vector(2, 4, 6, 8, 10)
```

### Pattern 4: For-Comprehension Integration

Because BitSet has `map`, `flatMap`, `filter`, and `foreach`:

```scala
for (bit <- bitSet) println(bit)

val doubled = for (b <- bitSet) yield b * 2   // returns BitSet

val pairs = for {
  a <- bitSet1
  b <- bitSet2
} yield a + b  // returns BitSet
```

### Pattern 5: Optimized Type-Preserving Operations

When `map(f: Int => Int)` is called, the result is a `BitSet`. When `map[B](f: Int => B)` would change the element type, the result is a `SortedSet[B]`. This is enforced via implicit `Ordering[B]` requirements with custom error messages:

```scala
BitSet(1, 2, 3).map(_ * 2)           // BitSet(2, 4, 6)
BitSet(1, 2, 3).map(_.toString)      // SortedSet("1", "2", "3")
BitSet(1, 2, 3).map(_ => "x")        // Error: No implicit Ordering[String]...
                                      // suggestion: upcast with .unsorted first
```

---

## 11. Analysis Axes Summary

### Axis 1: Mutability

| Aspect | Assessment |
|--------|------------|
| Approach | Separate immutable and mutable classes with shared base trait |
| Immutable semantics | Every modification returns a new BitSet; `this` returned when no actual change |
| Mutable semantics | In-place modification; returns `this.type` for chaining |
| Cross-conversion | `mutable.BitSet.toImmutable` and `immutable.BitSet.fromBitMask(mutable.toBitMask)` |
| Default | `collection.BitSet` companion defaults to immutable |

### Axis 2: Size Model

| Aspect | Assessment |
|--------|------------|
| Sizing | Dynamic in both variants |
| Growth | Mutable: doubles array; Immutable: allocates exactly-sized result |
| Shrinking | Immutable: automatic (picks smallest class); Mutable: never shrinks |
| Small-set optimization | Immutable: `BitSet1` (no array), `BitSet2` (no array); Mutable: none |
| Maximum | ~2.1 billion bits (`MaxSize * 64`) |

### Axis 3: Collection Interfaces

| Aspect | Assessment |
|--------|------------|
| Collection integration | **Richest of any language** -- full `Set[Int]` + `SortedSet[Int]` |
| Type hierarchy depth | 8+ levels of trait mixins |
| Generic collection ops | `map`, `flatMap`, `filter`, `foldLeft`, `zip`, etc. -- all work |
| Type preservation | `map(Int => Int)` returns BitSet; `map(Int => B)` returns `SortedSet[B]` |
| Predicate usage | `Set[A] extends (A => Boolean)` -- BitSet usable as `Int => Boolean` |
| Sorted guarantee | Elements always iterated in ascending `Int` order |
| `unsorted` | Upcast to `Set[Int]` (drops sorted requirement for operations) |

### Axis 4: Iteration Support

| Aspect | Assessment |
|--------|------------|
| Iterator | `Iterator[Int]` -- ascending order via `numberOfTrailingZeros` |
| `iteratorFrom` | Start iteration from given element (SortedSet contract) |
| `foreach` | Optimized word-by-word; skips zero words |
| For-comprehensions | Full support (`map`, `flatMap`, `withFilter`) |
| Parallel | `stepper` provides `IntStepper with EfficientSplit` |
| Lazy views | `.view` returns lazy `View` (inherited) |

### Axis 5: Operators & Syntactic Sugar

| Category | Operators |
|----------|-----------|
| Element add/remove (immutable) | `+`, `-` |
| Element add/remove (mutable) | `+=`, `-=` |
| Bulk add/remove | `++`, `++=`, `--`, `--=` |
| Set algebra (new result) | `&`, `\|`, `&~`, `^` |
| Set algebra (in-place, mutable) | `&=`, `\|=`, `&~=`, `^=` |
| Membership test | `apply` (`bitSet(5)`) |
| Comparison | `subsetOf`, `==`, `!=` |

### Axis 6: Serialization & Interop

| Aspect | Assessment |
|--------|------------|
| Raw export | `toBitMask: Array[Long]` |
| Raw import | `fromBitMask(Array[Long])`, `fromBitMaskNoCopy(Array[Long])` |
| Java serialization | Custom `SerializationProxy` (writes word count + longs) |
| Collection conversion | All standard: `toList`, `toSet`, `toArray`, `toVector`, `to(Factory)` |
| Cross-variant | `mutable.BitSet.toImmutable: immutable.BitSet` |
| Java stream interop | `stepper` for `IntStream` / parallel streams |
| String | `"BitSet(1, 3, 5)"` (standard Iterable toString) |

---

## 12. Comparison: Immutable vs. Mutable Side-by-Side

| Feature | `immutable.BitSet` | `mutable.BitSet` |
|---------|-------------------|------------------|
| Class kind | `sealed abstract class` | `class` |
| Storage | `Long` fields or `Array[Long]` | `var elems: Array[Long]` |
| Add element | `incl(elem): BitSet` / `+ elem` | `addOne(elem): this.type` / `+= elem` |
| Remove element | `excl(elem): BitSet` / `- elem` | `subtractOne(elem): this.type` / `-= elem` |
| Union | `\| other` returns new | `\|= other` modifies in place; `\| other` returns new |
| Intersection | `& other` returns new | `&= other` modifies in place; `& other` returns new |
| Difference | `&~ other` returns new | `&~= other` modifies in place; `&~ other` returns new |
| Sym. diff | `^ other` returns new | `^= other` modifies in place; `^ other` returns new |
| Clear | N/A (use `empty`) | `clear()` -- zeros array, retains capacity |
| Clone | N/A (immutable, sharing is safe) | `clone(): BitSet` -- deep copy |
| Small-set opt. | `BitSet1`, `BitSet2` | None |
| Convert to other | `toBitMask` then `mutable.BitSet.fromBitMask` | `toImmutable: immutable.BitSet` |
| Thread safety | Inherently safe (immutable) | Not thread-safe |
| `addAll` optimization | N/A | Specialized for `BitSet` (`\|=`), `Range` (bulk fill), `SortedSet` (pre-alloc) |
| `filterInPlace` | N/A | `filterInPlace(p): this.type` |
| `update` | N/A | `update(elem, Boolean): Unit` -- `set(5) = true` syntax |

---

## 13. Notable Design Decisions & Tradeoffs

1. **Non-negative integers only:** `require(elem >= 0)` is enforced. This is a fundamental constraint -- BitSet represents a set of natural numbers, not arbitrary values.

2. **`Set[Int]` not `Set[Boolean]` or bit-indexed:** Scala treats BitSet as a mathematical set of integers that happens to be stored efficiently as bits. The API is integer-centric, not bit-centric.

3. **No `flip`/`negate` operation:** Unlike `java.util.BitSet`, Scala BitSet has no `flip(index)` or `flip(fromIndex, toIndex)`. To flip, one must use `xor`.

4. **No `nextSetBit`/`nextClearBit`:** Low-level bit scanning is not exposed. Iteration via `iterator`/`foreach` is the intended interface.

5. **No `cardinality` -- use `size`:** Since BitSet is a `Set[Int]`, `size` serves the same purpose.

6. **No `get`/`set`/`clear` by index:** These Java-style methods are absent. Scala uses `contains`/`+`/`-` (immutable) or `contains`/`+=`/`-=` (mutable) instead.

7. **`unsorted` escape hatch:** When `map` to a non-`Int` type fails due to missing `Ordering`, the user is guided to call `.unsorted` first to upcast to `Set[Int]`, then use normal `Set` operations.

8. **`toImmutable` is one-way:** Only mutable has `toImmutable`. Going from immutable to mutable requires `mutable.BitSet.fromBitMask(immutable.toBitMask)` or building via `mutable.BitSet() ++= immutable`.

9. **No direct java.util.BitSet interop:** There is no `toJavaBitSet` or `fromJavaBitSet` conversion. Interop must go through `Array[Long]` manually.
