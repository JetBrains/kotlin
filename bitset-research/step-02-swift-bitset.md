# Swift `BitSet` — Cross-Language API Surface Analysis

**Source:** [apple/swift-collections](https://github.com/apple/swift-collections) package, `Sources/BitCollections/BitSet/` directory
**Package version:** swift-collections (latest `main` as of 2026-03-30)
**License:** Apache 2.0 with Runtime Library Exception
**Status:** Stable, part of Apple's official swift-collections package (NOT part of the Swift standard library)

---

## 1. Type Signature

```swift
public struct BitSet {
    internal var _storage: [_Word]
}
```

- **Value type** (struct), not a class.
- Internal storage is `[_Word]` where `_Word` wraps `UInt` (platform word size).
- Conforms to `Sendable` (safe for concurrent use by value).

### Conceptual Model

BitSet is described as:

> "A sorted collection of small nonnegative integers, implemented as an uncompressed bitmap of as many bits as the value of the largest member."

Key insight: the **Element type is `Int`**, not `Bool`. A BitSet is conceptualized as a **set of nonnegative integers**, not as a bit vector. The bit at position `n` being set means the integer `n` is a member of the set.

---

## 2. Protocol Conformances

This is the most architecturally significant aspect of Swift's BitSet design.

| Protocol | Conformance | Notes |
|---|---|---|
| `SetAlgebra` | Yes | THE primary protocol. `Element = Int`. Full set-theoretic API. |
| `Sequence` | Yes | Iterates over set members (Int values) in ascending order. |
| `Collection` | Yes | Random-access-like, but O(d) index advancement. |
| `BidirectionalCollection` | Yes | Forward and backward iteration over members. |
| `Equatable` | Yes | Compares underlying storage directly. |
| `Hashable` | Yes | Hashes the storage array. |
| `Codable` | Yes | Encodes as unkeyed container of `UInt64` values. Guarded by `#if !$Embedded`. |
| `ExpressibleByArrayLiteral` | Yes | `let bits: BitSet = [1, 2, 3]` |
| `CustomStringConvertible` | Yes | Array-like description: `[1, 2, 3]`. Guarded by `#if !$Embedded`. |
| `CustomDebugStringConvertible` | Yes | Same as `description`. Guarded by `#if !$Embedded`. |
| `CustomReflectable` | Yes | Mirror with `.set` display style. Guarded by `#if !$Embedded`. |
| `Sendable` | Yes | Value type, safe for concurrency. |
| `_SortedCollection` | Yes | Internal protocol. Provides O(1) `sorted()`, `min()`, `max()`. |
| `_UniqueCollection` | Yes | Internal protocol. Elements are unique. |

**Notable absences:**
- NOT `RandomAccessCollection` — index advancement is O(d) where d is the distance between members.
- NOT `RangeReplaceableCollection` — that role belongs to `BitArray`.
- NOT `MutableCollection` — mutations go through `insert`/`remove`/set algebra, not subscript assignment.

---

## 3. Nested Types

### 3.1. `BitSet.Index`

```swift
@frozen
public struct Index {
    var _value: UInt
}
```

- Opaque wrapper around `UInt` to prevent accidental integer arithmetic on indices.
- Conforms to: `Sendable`, `Equatable`, `Comparable`, `Hashable`, `CustomStringConvertible`, `CustomDebugStringConvertible`.
- Deliberately NOT `Int` even though elements are `Int`, to force users to use `index(after:)` etc.

### 3.2. `BitSet.Counted`

A wrapper around `BitSet` that maintains a cached `count` property for O(1) access.

```swift
extension BitSet {
    public struct Counted {
        internal var _bits: BitSet
        internal var _count: Int
    }
}
```

**Motivation:** `BitSet.count` is O(max) because it requires scanning all words and counting set bits (popcount). `BitSet.Counted` trades a small amount of space and per-mutation overhead for O(1) `count` and `isEmpty`.

**Conformances of `BitSet.Counted`:**

| Protocol | Notes |
|---|---|
| `SetAlgebra` | Full mirror of BitSet's SetAlgebra operations |
| `Sequence` | Same iterator as BitSet |
| `BidirectionalCollection` | Same as BitSet |
| `Equatable` | Short-circuits on count before comparing bits |
| `Hashable` | Same hash as underlying BitSet |
| `Codable` | Same encoding as BitSet |
| `ExpressibleByArrayLiteral` | Same as BitSet |
| `CustomStringConvertible` | Same as BitSet |
| `CustomDebugStringConvertible` | Same as BitSet |
| `CustomReflectable` | Same as BitSet |
| `Sendable` | Value type |
| `_SortedCollection` | O(1) sorted/min/max |
| `_UniqueCollection` | Elements unique |

**Conversion between BitSet and BitSet.Counted:**
```swift
// BitSet -> BitSet.Counted
let counted = bits.counted  // property, O(max) first time

// BitSet.Counted -> BitSet
let uncounted = counted.uncounted  // property, O(1)

// In-place mutation via uncounted
counted.uncounted.formUnion(other)  // auto-recounts
```

### 3.3. `BitSet.Iterator`

```swift
public struct Iterator: IteratorProtocol {
    // Iterates word by word, extracting set bit positions
    public mutating func next() -> Int?
}
```

- Conforms to `Sendable`.
- Iterates over members in ascending order.
- Each individual `next()` is worst-case O(n) but iterating the entire set is O(max).

---

## 4. Initializers

| Initializer | Complexity | Description |
|---|---|---|
| `init()` | O(1) | Empty bit set. |
| `init(words: some Sequence<UInt>)` | O(n) | From raw bitmap words. Bits counted LSB-first within each word. |
| `init(bitPattern: some BinaryInteger)` | O(bitWidth) | From any binary integer's word representation. |
| `init(_ array: BitArray)` | O(count) | From a BitArray (lossy — discards array length). |
| `init(_ elements: some Sequence<Int>)` | O(n) | From a sequence of nonnegative integers. |
| `init(_ range: Range<Int>)` | O(upperBound) | From an integer range. Must be nonnegative. |
| `init(_ bits: BitSet.Counted)` | O(1) | Strip count tracking. |
| `init(reservingCapacity: Int)` | O(1) | Empty set with preallocated storage for values up to the given max. |

---

## 5. Properties

| Property | Type | Complexity | Description |
|---|---|---|---|
| `isEmpty` | `Bool` | O(min) | Whether the set has no members. |
| `count` | `Int` | O(max) | Number of set bits (popcount over all words). **O(1) in `Counted` variant.** |
| `startIndex` | `Index` | O(min) | Position of the first member. |
| `endIndex` | `Index` | O(1) | Past-the-end position. |
| `underestimatedCount` | `Int` | O(max) | Returns exact `count` (since it's known to be exact). |
| `first` | `Int?` | O(min) | Smallest member (from Collection). |
| `last` | `Int?` | O(1) | Largest member (from BidirectionalCollection). |
| `counted` | `BitSet.Counted` | O(max) | Wrapped version that tracks count. |
| `description` | `String` | O(n) | String representation like `[1, 2, 3]`. |
| `debugDescription` | `String` | O(n) | Same as description. |
| `customMirror` | `Mirror` | O(n) | Mirror with `.set` display style. |

---

## 6. Core Mutation Operations (SetAlgebra basics)

| Method | Signature | Returns | Complexity | Description |
|---|---|---|---|---|
| `contains(_:)` | `(Int) -> Bool` | `Bool` | O(1) | Membership test. Returns `false` for negative values. |
| `insert(_:)` | `(Int) -> (inserted: Bool, memberAfterInsert: Int)` | Tuple | O(1) amortized | Insert a nonnegative integer. Precondition failure for negative. |
| `update(with:)` | `(Int) -> Int?` | `Int?` | O(1) amortized | Insert unconditionally. Returns old value if was present. |
| `remove(_:)` | `(Int) -> Int?` | `Int?` | O(1) amortized | Remove a member. Returns the value if it was present, nil otherwise. |
| `remove(at:)` | `(Index) -> Int` | `Int` | O(1) amortized | Remove the element at a specific index. |
| `filter(_:)` | `((Int) throws -> Bool) rethrows -> BitSet` | `BitSet` | O(n) | Returns a new BitSet with only elements satisfying the predicate. |

**Note on amortized O(1):** Insert/remove are O(1) when the set is uniquely referenced and the value fits within existing capacity. Otherwise O(max(newValue, currentMax)) for CoW copy or growth.

---

## 7. Set Algebra Operations

### 7.1. Set-Returning Operations

Each operation has **four overloads** accepting different parameter types:

| Method | Parameter types | Returns |
|---|---|---|
| `union(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `BitSet` |
| `intersection(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `BitSet` |
| `symmetricDifference(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `BitSet` |
| `subtracting(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `BitSet` |

**Complexity:** O(max) for BitSet-to-BitSet operations. O(max) + O(k) for sequence variants where k is the iteration cost.

**Implementation detail:** BitSet-to-BitSet operations use word-level bitwise operations (`|`, `&`, `^`, `& ~`) for maximum performance.

### 7.2. In-Place Mutations

Same four overloads for each:

| Method | Parameter types | Mutates self |
|---|---|---|
| `formUnion(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | Yes |
| `formIntersection(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | Yes |
| `formSymmetricDifference(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | Yes |
| `subtract(_:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | Yes |

### 7.3. Set Relation Queries

Same four overloads for each:

| Method | Parameter types | Returns |
|---|---|---|
| `isSubset(of:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |
| `isSuperset(of:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |
| `isStrictSubset(of:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |
| `isStrictSuperset(of:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |
| `isDisjoint(with:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |
| `isEqualSet(to:)` | `BitSet`, `BitSet.Counted`, `Range<Int>`, `some Sequence<Int>` | `Bool` |

**`isEqualSet(to:)`** is non-standard — not part of `SetAlgebra`, but an extension that generalizes `==` to heterogeneous comparisons.

### 7.4. Total count of set algebra method overloads

14 base methods x 4 overloads each = **56 public set algebra methods** (plus `==` from `Equatable` which delegates to `isEqualSet(to:)`).

---

## 8. Subscripts

| Subscript | Signature | Access | Description |
|---|---|---|---|
| `subscript(position:)` | `(Index) -> Int` | get | Collection subscript. Returns the member at a given index. |
| `subscript(member:)` | `(member: Int) -> Bool` | get/set | Convenience: `bits[member: 5] = true` is equivalent to `bits.insert(5)`. Supports `.toggle()`. |
| `subscript(members:)` | `(members: Range<Int>) -> Slice<BitSet>` | get | Slicing: returns members within an integer range. |
| `subscript(members:)` | `(members: some RangeExpression<Int>) -> Slice<BitSet>` | get | Generic range expression variant. |

The `subscript(member:)` is a notable design feature — it bridges the mental model gap between "set of integers" and "vector of booleans":
```swift
var bits: BitSet = [1, 2, 3]
bits[member: 4] = true   // insert
bits[member: 2] = false  // remove
bits[member: 5].toggle() // toggle membership
```

---

## 9. Collection Navigation

| Method | Signature | Complexity | Description |
|---|---|---|---|
| `index(after:)` | `(Index) -> Index` | O(d) | Next member position. d = distance between consecutive members. |
| `index(before:)` | `(Index) -> Index` | O(d) | Previous member position. |
| `distance(from:to:)` | `(Index, Index) -> Int` | O(d) | Number of members between two indices. |
| `index(_:offsetBy:)` | `(Index, Int) -> Index` | O(d) | Offset an index by a number of positions. |
| `index(_:offsetBy:limitedBy:)` | `(Index, Int, Index) -> Index?` | O(d) | Offset with limit. |
| `makeIterator()` | `() -> Iterator` | O(1) | Get an iterator. |

---

## 10. Sorted Collection APIs

BitSet elements are **always in ascending sorted order** (inherent to the bitmap representation).

| Method | Signature | Complexity | Description |
|---|---|---|---|
| `sorted()` | `() -> BitSet` | O(1) | Returns self (already sorted). |
| `min()` | `() -> Int?` | O(1) | Returns `first` — the smallest member. |
| `max()` | `() -> Int?` | O(1) | Returns `last` — the largest member. |

---

## 11. Capacity Management

| Method/Init | Signature | Description |
|---|---|---|
| `init(reservingCapacity:)` | `(Int)` | Create empty set with capacity to store values up to the given maximum. |
| `reserveCapacity(_:)` | `mutating (Int)` | Ensure storage can hold values up to the given maximum. |

**Note:** The capacity parameter is `maximumValue`, not element count. `reserveCapacity(1000)` allocates enough words to represent bit 1000, not 1000 elements.

---

## 12. Random Generation

| Method | Signature | Description |
|---|---|---|
| `BitSet.random(upTo:)` | `static (Int) -> BitSet` | Random set with members in `0..<limit`. |
| `BitSet.random(upTo:using:)` | `static (Int, inout RNG) -> BitSet` | Random set with custom RNG. |

---

## 13. Codable Encoding

Encoded as an **unkeyed container of `UInt64` values** — NOT `UInt` (platform word size), but explicitly `UInt64` for cross-platform stability. This means serialized data is portable across 32-bit and 64-bit platforms.

```swift
// Encoding: unkeyed container of UInt64 values
// Example: BitSet([0, 2, 64]) encodes as [5, 1] (two UInt64 values)
```

---

## 14. Operators

Swift's `SetAlgebra` protocol does NOT define operators like `|`, `&`, `^`, `~`. Set operations are done exclusively via named methods. There are **no bitwise operators** on `BitSet`.

The only operator is `==` from `Equatable`.

---

## 15. BitArray Comparison

The swift-collections package includes a separate `BitArray` type alongside `BitSet`.

| Aspect | `BitSet` | `BitArray` |
|---|---|---|
| **Conceptual model** | Set of nonnegative integers | Dense array of Bool values |
| **Element type** | `Int` (set member values) | `Bool` |
| **Size** | Dynamic, grows to fit largest member | Fixed/explicit count |
| **Length semantics** | No concept of "length" — only members | Has explicit `count` |
| **Trailing zeros** | Trimmed from storage | Preserved (count tracked) |
| **Primary protocols** | `SetAlgebra`, `Collection` | `MutableCollection`, `RandomAccessCollection`, `RangeReplaceableCollection` |
| **Subscript** | `[member: n]` -> Bool (set membership) | `[n]` -> Bool (array element) |
| **Mutation** | `insert`/`remove` | Subscript assignment `bits[i] = true` |
| **Iteration** | Over member indices (Int) | Over boolean values (Bool) |
| **Bitwise ops** | Set algebra methods | Bitwise operators available via `BitwiseOperations` |

**Key design insight:** Swift explicitly separates the "set of integers" use case (BitSet) from the "vector of booleans" use case (BitArray), giving each its own type with appropriate semantics and protocol conformances.

**Conversion:**
```swift
let set = BitSet([0, 2, 5])
let array = BitArray(set)      // [true, false, true, false, false, true]
let set2 = BitSet(array)       // lossy: discards array length
```

---

## 16. Cross-Cutting Analysis

### 16.1. Mutability Model

- **Value type** (struct) with **copy-on-write** (CoW) semantics.
- Storage is a Swift `Array<_Word>` which itself is CoW.
- Mutating methods are marked `mutating` — compile-time enforcement.
- No separate mutable/immutable types (unlike Java's Collections.unmodifiable pattern).
- Mutations are in-place when uniquely referenced, otherwise copy.

### 16.2. Size Model

- **Dynamic/unbounded.** Storage grows automatically to accommodate the largest member.
- Storage shrinks after removals via `_shrink()` (trims trailing empty words).
- No fixed-size variant.
- Pre-allocation via `reserveCapacity(_:)`.

### 16.3. Collection Interfaces

- **Primary identity:** `SetAlgebra` — the set operations are first-class.
- **Secondary identity:** `BidirectionalCollection` — enables `for member in bits`, `bits.first`, `bits.last`, slicing.
- NOT `RandomAccessCollection` because index advancement between sparse elements is O(d).
- Iteration order: always ascending (smallest to largest member).
- The `Sequence`/`Collection` conformance iterates over **member values** (Int), not bits.

### 16.4. Operators and Syntactic Sugar

- **No bitwise operators** (`|`, `&`, `^`, `~`). Only named methods.
- The `subscript(member:)` provides syntactic sugar: `bits[member: n]` for get/set/toggle.
- `ExpressibleByArrayLiteral`: `let bits: BitSet = [1, 3, 5]`.
- Swift's `SetAlgebra` does not come with operators — this is a deliberate Swift standard library design choice.

### 16.5. Serialization and Interop

- **Codable:** Yes, using `UInt64` words for cross-platform portability.
- **From raw words:** `init(words: some Sequence<UInt>)` — interop with platform-native word arrays.
- **From BinaryInteger:** `init(bitPattern: some BinaryInteger)` — any integer type.
- **From/to BitArray:** bidirectional (lossy in one direction).
- **No direct conversion to/from `[Int]`** — but `Array(bitSet)` works via `Collection` conformance.
- **No conversion to/from byte arrays** explicitly (unlike Java's `BitSet.toByteArray()`/`valueOf(byte[])`).

### 16.6. Thread Safety

- `Sendable` conformance — safe to send across concurrency boundaries as a value type.
- No locking needed — independent copies via CoW.

---

## 17. Design Decisions Summary Table

| Decision | Swift BitSet choice | Rationale |
|---|---|---|
| Type kind | `struct` (value type) | Swift idiom, CoW gives performance + safety |
| Element type | `Int` | Models set membership; negative values rejected |
| Primary protocol | `SetAlgebra` | Set-theoretic identity |
| Collection conformance | `BidirectionalCollection` | Iteration is natural but not O(1)-index |
| Count caching | Separate `Counted` wrapper | Avoids overhead for users who don't need O(1) count |
| Separate BitArray | Yes | Clean separation of set vs. vector semantics |
| Bitwise operators | None | Named methods preferred in Swift's set algebra design |
| Serialization format | `UInt64` words | Platform-independent |
| Word type | `UInt` (platform word size) | Fastest native operations |
| Negative values | Precondition failure | Nonnegative-only by design |
| Capacity semantics | `maximumValue` not `elementCount` | Meaningful for bitmap representation |

---

## 18. Relevance to Kotlin BitSet Design

### What to adopt from Swift's design:

1. **SetAlgebra-first identity.** Swift treats BitSet as a set, not a bit vector. For Kotlin, this maps to implementing relevant set interfaces rather than just providing Java BitSet's procedural API.

2. **Counted variant pattern.** The `BitSet.Counted` approach of offering an optional O(1) count wrapper is elegant — avoids burdening the base type with count-tracking overhead while serving users who need it.

3. **`subscript(member:)` pattern.** The `bits[member: n]` subscript provides excellent ergonomics for toggle/set/clear operations. Kotlin's `operator get/set` could serve a similar role.

4. **Overloads for Range and Sequence.** Every set operation accepts `BitSet`, `Range<Int>`, and `Sequence<Int>`, providing flexibility without forcing conversions.

5. **Separation of BitSet and BitArray.** Clean conceptual split between "set of integers" and "dense boolean vector" — could inform whether Kotlin stdlib needs one type or two.

### What differs from Kotlin's context:

1. **Swift has `SetAlgebra` protocol.** Kotlin has no direct equivalent — closest is implementing `MutableSet<Int>` or `MutableIterable<Int>`, but these carry different expectations.

2. **Value type vs. reference type.** Swift structs with CoW are natural; Kotlin classes are reference types. Kotlin's immutable/mutable split (like `List`/`MutableList`) would be the idiomatic equivalent.

3. **No bitwise operators in Swift's SetAlgebra.** Kotlin could choose differently — `infix fun or(other: BitSet)` or `operator` overloads are idiomatic in Kotlin.

4. **Codable vs. Serializable.** Different serialization ecosystems, but the `UInt64`-based encoding for portability is a good idea regardless.
