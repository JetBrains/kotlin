/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.util;

import com.intellij.util.containers.ConcurrentLongObjectMap;
import com.intellij.util.containers.ThreadLocalRandom;
import com.intellij.util.containers.Unsafe;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * DO NOT MODIFY. THIS FILE WAS PORTED FROM THE OTHER REPOSITORY AND SHOULD BE TREATED AS GENERATED
 * Adapted from Doug Lea <a href="https://gee.cs.oswego.edu/dl/concurrency-interest/index.html">ConcurrentHashMap</a> to long keys
 * with following additions/changes:
 * - added hashing strategy argument
 * - added cacheOrGet convenience method
 * - Null values are NOT allowed
 * @author Doug Lea
 * @param <V> the type of mapped values
 * Use {@link com.intellij.concurrency.ConcurrentCollectionFactory#createConcurrentLongObjectMap()} to create this map
 */
@SuppressWarnings("ALL")
final class ConcurrentLongObjectHashMap<V> implements ConcurrentLongObjectMap<V> {

  /*
   * Overview:
   *
   * The primary design goal of this hash table is to maintain
   * concurrent readability (typically method get(), but also
   * iterators and related methods) while minimizing update
   * contention. Secondary goals are to keep space consumption about
   * the same or better than java.util.HashMap, and to support high
   * initial insertion rates on an empty table by many threads.
   *
   * This map usually acts as a binned (bucketed) hash table.  Each
   * key-value mapping is held in a Node.  Most nodes are instances
   * of the basic Node class with hash, key, value, and next
   * fields. However, various subclasses exist: TreeNodes are
   * arranged in balanced trees, not lists.  TreeBins hold the roots
   * of sets of TreeNodes. ForwardingNodes are placed at the heads
   * of bins during resizing. ReservationNodes are used as
   * placeholders while establishing values in computeIfAbsent and
   * related methods.  The types TreeBin, ForwardingNode, and
   * ReservationNode do not hold normal user keys, values, or
   * hashes, and are readily distinguishable during search etc
   * because they have negative hash fields and null key and value
   * fields. (These special nodes are either uncommon or transient,
   * so the impact of carrying around some unused fields is
   * insignificant.)
   *
   * The table is lazily initialized to a power-of-two size upon the
   * first insertion.  Each bin in the table normally contains a
   * list of Nodes (most often, the list has only zero or one Node).
   * Table accesses require volatile/atomic reads, writes, and
   * CASes.  Because there is no other way to arrange this without
   * adding further indirections, we use intrinsics
   * (jdk.internal.misc.Unsafe) operations.
   *
   * We use the top (sign) bit of Node hash fields for control
   * purposes -- it is available anyway because of addressing
   * constraints.  Nodes with negative hash fields are specially
   * handled or ignored in map methods.
   *
   * Insertion (via put or its variants) of the first node in an
   * empty bin is performed by just CASing it to the bin.  This is
   * by far the most common case for put operations under most
   * key/hash distributions.  Other update operations (insert,
   * delete, and replace) require locks.  We do not want to waste
   * the space required to associate a distinct lock object with
   * each bin, so instead use the first node of a bin list itself as
   * a lock. Locking support for these locks relies on builtin
   * "synchronized" monitors.
   *
   * Using the first node of a list as a lock does not by itself
   * suffice though: When a node is locked, any update must first
   * validate that it is still the first node after locking it, and
   * retry if not. Because new nodes are always appended to lists,
   * once a node is first in a bin, it remains first until deleted
   * or the bin becomes invalidated (upon resizing).
   *
   * The main disadvantage of per-bin locks is that other update
   * operations on other nodes in a bin list protected by the same
   * lock can stall, for example when user equals() or mapping
   * functions take a long time.  However, statistically, under
   * random hash codes, this is not a common problem.  Ideally, the
   * frequency of nodes in bins follows a Poisson distribution
   * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
   * parameter of about 0.5 on average, given the resizing threshold
   * of 0.75, although with a large variance because of resizing
   * granularity. Ignoring variance, the expected occurrences of
   * list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The
   * first values are:
   *
   * 0:    0.60653066
   * 1:    0.30326533
   * 2:    0.07581633
   * 3:    0.01263606
   * 4:    0.00157952
   * 5:    0.00015795
   * 6:    0.00001316
   * 7:    0.00000094
   * 8:    0.00000006
   * more: less than 1 in ten million
   *
   * Lock contention probability for two threads accessing distinct
   * elements is roughly 1 / (8 * #elements) under random hashes.
   *
   * Actual hash code distributions encountered in practice
   * sometimes deviate significantly from uniform randomness.  This
   * includes the case when N > (1<<30), so some keys MUST collide.
   * Similarly for dumb or hostile usages in which multiple keys are
   * designed to have identical hash codes or ones that differs only
   * in masked-out high bits. So we use a secondary strategy that
   * applies when the number of nodes in a bin exceeds a
   * threshold. These TreeBins use a balanced tree to hold nodes (a
   * specialized form of red-black trees), bounding search time to
   * O(log N).  Each search step in a TreeBin is at least twice as
   * slow as in a regular list, but given that N cannot exceed
   * (1<<64) (before running out of addresses) this bounds search
   * steps, lock hold times, etc, to reasonable constants (roughly
   * 100 nodes inspected per operation worst case) so long as keys
   * are Comparable (which is very common -- String, Long, etc).
   * TreeBin nodes (TreeNodes) also maintain the same "next"
   * traversal pointers as regular nodes, so can be traversed in
   * iterators in the same way.
   *
   * The table is resized when occupancy exceeds a percentage
   * threshold (nominally, 0.75, but see below).  Any thread
   * noticing an overfull bin may assist in resizing after the
   * initiating thread allocates and sets up the replacement array.
   * However, rather than stalling, these other threads may proceed
   * with insertions etc.  The use of TreeBins shields us from the
   * worst case effects of overfilling while resizes are in
   * progress.  Resizing proceeds by transferring bins, one by one,
   * from the table to the next table. However, threads claim small
   * blocks of indices to transfer (via field transferIndex) before
   * doing so, reducing contention.  A generation stamp in field
   * sizeCtl ensures that resizings do not overlap. Because we are
   * using power-of-two expansion, the elements from each bin must
   * either stay at same index, or move with a power of two
   * offset. We eliminate unnecessary node creation by catching
   * cases where old nodes can be reused because their next fields
   * won't change.  On average, only about one-sixth of them need
   * cloning when a table doubles. The nodes they replace will be
   * garbage collectible as soon as they are no longer referenced by
   * any reader thread that may be in the midst of concurrently
   * traversing table.  Upon transfer, the old table bin contains
   * only a special forwarding node (with hash field "MOVED") that
   * contains the next table as its key. On encountering a
   * forwarding node, access and update operations restart, using
   * the new table.
   *
   * Each bin transfer requires its bin lock, which can stall
   * waiting for locks while resizing. However, because other
   * threads can join in and help resize rather than contend for
   * locks, average aggregate waits become shorter as resizing
   * progresses.  The transfer operation must also ensure that all
   * accessible bins in both the old and new table are usable by any
   * traversal.  This is arranged in part by proceeding from the
   * last bin (table.length - 1) up towards the first.  Upon seeing
   * a forwarding node, traversals (see class Traverser) arrange to
   * move to the new table without revisiting nodes.  To ensure that
   * no intervening nodes are skipped even when moved out of order,
   * a stack (see class TableStack) is created on first encounter of
   * a forwarding node during a traversal, to maintain its place if
   * later processing the current table. The need for these
   * save/restore mechanics is relatively rare, but when one
   * forwarding node is encountered, typically many more will be.
   * So Traversers use a simple caching scheme to avoid creating so
   * many new TableStack nodes. (Thanks to Peter Levart for
   * suggesting use of a stack here.)
   *
   * The traversal scheme also applies to partial traversals of
   * ranges of bins (via an alternate Traverser constructor)
   * to support partitioned aggregate operations.  Also, read-only
   * operations give up if ever forwarded to a null table, which
   * provides support for shutdown-style clearing, which is also not
   * currently implemented.
   *
   * Lazy table initialization minimizes footprint until first use,
   * and also avoids resizings when the first operation is from a
   * putAll, constructor with map argument, or deserialization.
   * These cases attempt to override the initial capacity settings,
   * but harmlessly fail to take effect in cases of races.
   *
   * The element count is maintained using a specialization of
   * LongAdder. We need to incorporate a specialization rather than
   * just use a LongAdder in order to access implicit
   * contention-sensing that leads to creation of multiple
   * CounterCells.  The counter mechanics avoid contention on
   * updates but can encounter cache thrashing if read too
   * frequently during concurrent access. To avoid reading so often,
   * resizing under contention is attempted only upon adding to a
   * bin already holding two or more nodes. Under uniform hash
   * distributions, the probability of this occurring at threshold
   * is around 13%, meaning that only about 1 in 8 puts check
   * threshold (and after resizing, many fewer do so).
   *
   * TreeBins use a special form of comparison for search and
   * related operations (which is the main reason we cannot use
   * existing collections such as TreeMaps). TreeBins contain
   * Comparable elements, but may contain others, as well as
   * elements that are Comparable but not necessarily Comparable for
   * the same T, so we cannot invoke compareTo among them. To handle
   * this, the tree is ordered primarily by hash value, then by
   * Comparable.compareTo order if applicable.  On lookup at a node,
   * if elements are not comparable or compare as 0 then both left
   * and right children may need to be searched in the case of tied
   * hash values. (This corresponds to the full list search that
   * would be necessary if all elements were non-Comparable and had
   * tied hashes.) On insertion, to keep a total ordering (or as
   * close as is required here) across rebalancings, we compare
   * classes and identityHashCodes as tie-breakers. The red-black
   * balancing code is updated from pre-jdk-collections
   * (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java)
   * based in turn on Cormen, Leiserson, and Rivest "Introduction to
   * Algorithms" (CLR).
   *
   * TreeBins also require an additional locking mechanism.  While
   * list traversal is always possible by readers even during
   * updates, tree traversal is not, mainly because of tree-rotations
   * that may change the root node and/or its linkages.  TreeBins
   * include a simple read-write lock mechanism parasitic on the
   * main bin-synchronization strategy: Structural adjustments
   * associated with an insertion or removal are already bin-locked
   * (and so cannot conflict with other writers) but must wait for
   * ongoing readers to finish. Since there can be only one such
   * waiter, we use a simple scheme using a single "waiter" field to
   * block writers.  However, readers need never block.  If the root
   * lock is held, they proceed along the slow traversal path (via
   * next-pointers) until the lock becomes available or the list is
   * exhausted, whichever comes first. These cases are not fast, but
   * maximize aggregate expected throughput.
   *
   * Maintaining API and serialization compatibility with previous
   * versions of this class introduces several oddities. Mainly: We
   * leave untouched but unused constructor arguments referring to
   * concurrencyLevel. We accept a loadFactor constructor argument,
   * but apply it only to initial table capacity (which is the only
   * time that we can guarantee to honor it.) We also declare an
   * unused "Segment" class that is instantiated in minimal form
   * only when serializing.
   *
   * Also, solely for compatibility with previous versions of this
   * class, it extends AbstractMap, even though all of its methods
   * are overridden, so it is just useless baggage.
   *
   * This file is organized to make things a little easier to follow
   * while reading than they might otherwise: First the main static
   * declarations and utilities, then fields, then main public
   * methods (with a few factorings of multiple public methods into
   * internal ones), then sizing methods, trees, traversers, and
   * bulk operations.
   */

  /* ---------------- Constants -------------- */

  /**
   * The largest possible table capacity.  This value must be
   * exactly 1<<30 to stay within Java array allocation and indexing
   * bounds for power of two table sizes, and is further required
   * because the top two bits of 32bit hash fields are used for
   * control purposes.
   */
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  /**
   * The default initial table capacity.  Must be a power of 2
   * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
   */
  private static final int DEFAULT_CAPACITY = 16;

  /**
   * The largest possible (non-power of two) array size.
   * Needed by toArray and related methods.
   */
  static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

  /**
   * The default concurrency level for this table. Unused but
   * defined for compatibility with previous versions of this class.
   */
  private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

  /**
   * The load factor for this table. Overrides of this value in
   * constructors affect only the initial table capacity.  The
   * actual floating point value isn't normally used -- it is
   * simpler to use expressions such as {@code n - (n >>> 2)} for
   * the associated resizing threshold.
   */
  private static final float LOAD_FACTOR = 0.75f;

  /**
   * The bin count threshold for using a tree rather than list for a
   * bin.  Bins are converted to trees when adding an element to a
   * bin with at least this many nodes. The value must be greater
   * than 2, and should be at least 8 to mesh with assumptions in
   * tree removal about conversion back to plain bins upon
   * shrinkage.
   */
  static final int TREEIFY_THRESHOLD = 8;

  /**
   * The bin count threshold for untreeifying a (split) bin during a
   * resize operation. Should be less than TREEIFY_THRESHOLD, and at
   * most 6 to mesh with shrinkage detection under removal.
   */
  static final int UNTREEIFY_THRESHOLD = 6;

  /**
   * The smallest table capacity for which bins may be treeified.
   * (Otherwise the table is resized if too many nodes in a bin.)
   * The value should be at least 4 * TREEIFY_THRESHOLD to avoid
   * conflicts between resizing and treeification thresholds.
   */
  static final int MIN_TREEIFY_CAPACITY = 64;

  /**
   * Minimum number of rebinnings per transfer step. Ranges are
   * subdivided to allow multiple resizer threads.  This value
   * serves as a lower bound to avoid resizers encountering
   * excessive memory contention.  The value should be at least
   * DEFAULT_CAPACITY.
   */
  private static final int MIN_TRANSFER_STRIDE = 16;

  /**
   * The number of bits used for generation stamp in sizeCtl.
   * Must be at least 6 for 32bit arrays.
   */
  private static final int RESIZE_STAMP_BITS = 16;

  /**
   * The maximum number of threads that can help resize.
   * Must fit in 32 - RESIZE_STAMP_BITS bits.
   */
  private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

  /**
   * The bit shift for recording size stamp in sizeCtl.
   */
  private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

  /*
   * Encodings for Node hash fields. See above for explanation.
   */
  static final int MOVED     = -1; // hash for forwarding nodes
  static final int TREEBIN   = -2; // hash for roots of trees
  static final int RESERVED  = -3; // hash for transient reservations
  static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

  /** Number of CPUS, to place bounds on some sizings */
  static final int NCPU = Runtime.getRuntime().availableProcessors();

  /* ---------------- Nodes -------------- */

  /**
   * Key-value entry.  This class is never exported out as a
   * user-mutable Map.Entry (i.e., one supporting setValue; see
   * MapEntry below), but can be used for read-only traversals used
   * in bulk tasks.  Subclasses of Node with a negative hash field
   * are special, and contain null keys and values (but are never
   * exported).  Otherwise, keys and vals are never null.
   */
  static class Node<V> implements LongEntry<V> {
    final int hash;
    final long key;
    volatile V val;
    volatile Node<V> next;

    Node(int hash, long key, V val, Node<V> next) {
      this.hash = hash;
      this.key = key;
      this.val = val;
      this.next = next;
    }

    @Override
    public final long getKey() {
      return key;
    }

    @NotNull
    @Override
    public final V getValue() {
      return val;
    }

    @Override
    public final int hashCode() {
      return spread(key) ^ val.hashCode();
    }

    @Override
    public final String toString() {
      return key + "=" + val;
    }

    @Override
    public final boolean equals(Object o) {
      if (!(o instanceof LongEntry<?>)) return false;
      LongEntry<?> e = (LongEntry<?>)o; 
      if (e.getKey() != key) return false;
      Object v = e.getValue();
      Object u = val;
      return v == u || v.equals(u);
    }

    /**
     * Virtualized support for map.get(); overridden in subclasses.
     */
    Node<V> find(int h, long k) {
      Node<V> e = this;
      do {
        if ((e.key == k)) {
          return e;
        }
      }
      while ((e = e.next) != null);
      return null;
    }
  }

  /* ---------------- Static utilities -------------- */

  /**
   * Spreads (XORs) higher bits of hash to lower and also forces top
   * bit to 0. Because the table uses power-of-two masking, sets of
   * hashes that vary only in bits above the current mask will
   * always collide. (Among known examples are sets of Float keys
   * holding consecutive whole numbers in small tables.)  So we
   * apply a transform that spreads the impact of higher bits
   * downward. There is a tradeoff between speed, utility, and
   * quality of bit-spreading. Because many common sets of hashes
   * are already reasonably distributed (so don't benefit from
   * spreading), and because we use trees to handle large sets of
   * collisions in bins, we just XOR some shifted bits in the
   * cheapest possible way to reduce systematic lossage, as well as
   * to incorporate impact of the highest bits that would otherwise
   * never be used in index calculations because of table bounds.
   */
  static final int spread(long h) {
      h = h ^ (h >>> 32);
      return (int)(h ^ (h >>> 16)) & HASH_BITS;
  }

  /**
   * Returns a power of two table size for the given desired capacity.
   * See Hackers Delight, sec 3.2
   */
  private static final int tableSizeFor(int c) {
      int n = -1 >>> Integer.numberOfLeadingZeros(c - 1);
      return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
  }


  /* ---------------- Table element access -------------- */

  /*
   * Atomic access methods are used for table elements as well as
   * elements of in-progress next table while resizing.  All uses of
   * the tab arguments must be null checked by callers.  All callers
   * also paranoically precheck that tab's length is not zero (or an
   * equivalent check), thus ensuring that any index argument taking
   * the form of a hash value anded with (length - 1) is a valid
   * index.  Note that, to be correct wrt arbitrary concurrency
   * errors by users, these checks must operate on local variables,
   * which accounts for some odd-looking inline assignments below.
   * Note that calls to setTabAt always occur within locked regions,
   * and so require only release ordering.
   */
  static <V> Node<V> tabAt(Node<V>[] tab, int i) {
    try {
      Object o = Unsafe.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
      return (Node<V>)o;
    }
    catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  static <V> boolean casTabAt(Node<V>[] tab, int i,
                              Node<V> v) {
    try {
      return Unsafe.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, null, v);
    }
    catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }

  static <V> void setTabAt(Node<V>[] tab, int i, Node<V> v) {
    try {
      Unsafe.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }
    catch (Throwable throwable) {
      throw new RuntimeException(throwable);
    }
  }


  /* ---------------- Fields -------------- */

  /**
   * The array of bins. Lazily initialized upon first insertion.
   * Size is always a power of two. Accessed directly by iterators.
   */
  transient volatile Node<V>[] table;

  /**
   * The next table to use; non-null only while resizing.
   */
  private transient volatile Node<V>[] nextTable;

  /**
   * Base counter value, used mainly when there is no contention,
   * but also as a fallback during table initialization
   * races. Updated via CAS.
   */
  private transient volatile long baseCount;

  /**
   * Table initialization and resizing control.  When negative, the
   * table is being initialized or resized: -1 for initialization,
   * else -(1 + the number of active resizing threads).  Otherwise,
   * when table is null, holds the initial table size to use upon
   * creation, or 0 for default. After initialization, holds the
   * next element count value upon which to resize the table.
   */
  private transient volatile int sizeCtl;

  /**
   * The next table index (plus one) to split while resizing.
   */
  private transient volatile int transferIndex;

  /**
   * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
   */
  private transient volatile int cellsBusy;

  /**
   * Table of counter cells. When non-null, size is a power of 2.
   */
  private transient volatile CounterCell[] counterCells;

  // views
  private transient ValuesView<V> values;
  private transient EntrySetView<V> entrySet;


  /* ---------------- Public operations -------------- */

  /**
   * Creates a new, empty map with the default initial table size (16).
   */
  ConcurrentLongObjectHashMap() {
  }

  /**
   * Creates a new, empty map with an initial table size
   * accommodating the specified number of elements without the need
   * to dynamically resize.
   *
   * @param initialCapacity The implementation performs internal
   * sizing to accommodate this many elements.
   * @throws IllegalArgumentException if the initial capacity of
   * elements is negative
   */
  ConcurrentLongObjectHashMap(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException();
    }
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
               MAXIMUM_CAPACITY :
               tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    sizeCtl = cap;
  }

  // Original (since JDK1.2) Map methods

  /**
   * {@inheritDoc}
   */
  public int size() {
      long n = sumCount();
      return ((n < 0L) ? 0 :
              (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
              (int)n);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isEmpty() {
      return sumCount() <= 0L; // ignore transient negative values
  }

  /**
   * Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code key.equals(k)},
   * then this method returns {@code v}; otherwise it returns
   * {@code null}.  (There can be at most one such mapping.)
   *
   * @throws NullPointerException if the specified key is null
   */
  public V get(long key) {
      Node<V>[] tab; Node<V> e, p; int n, eh;
    int h = spread(key);
      if ((tab = table) != null && (n = tab.length) > 0 &&
          (e = tabAt(tab, (n - 1) & h)) != null) {
          if ((eh = e.hash) == h) {
            if (e.key == key) {
              return e.val;
            }
          }
          else if (eh < 0) {
            return (p = e.find(h, key)) != null ? p.val : null;
          }
          while ((e = e.next) != null) {
            if (e.hash == h &&
                (e.key == key)) {
              return e.val;
            }
          }
      }
      return null;
  }

  /**
   * Tests if the specified object is a key in this table.
   *
   * @param  key possible key
   * @return {@code true} if and only if the specified object
   *         is a key in this table, as determined by the
   *         {@code equals} method; {@code false} otherwise
   * @throws NullPointerException if the specified key is null
   */
  public boolean containsKey(long key) {
      return get(key) != null;
  }

  /**
   * Returns {@code true} if this map maps one or more keys to the
   * specified value. Note: This method may require a full traversal
   * of the map, and is much slower than method {@code containsKey}.
   *
   * @param value value whose presence in this map is to be tested
   * @return {@code true} if this map maps one or more keys to the
   * specified value
   */
  @Override
  public boolean containsValue(@NotNull Object value) {
    Node<V>[] t;
    if ((t = table) != null) {
      Traverser<V> it = new Traverser<>(t, t.length, 0, t.length);
      for (Node<V> p; (p = it.advance()) != null; ) {
        V v;
        if ((v = p.val) == value || (v != null && value.equals(v))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Maps the specified key to the specified value in this table.
   * Neither the key nor the value can be null.
   *
   * <p>The value can be retrieved by calling the {@code get} method
   * with a key that is equal to the original key.
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with {@code key}, or
   * {@code null} if there was no mapping for {@code key}
   */
  @Override
  public V put(long key, @NotNull V value) {
    return putVal(key, value, false);
  }

  /** Implementation for put and putIfAbsent */
  final V putVal(long key, V value, boolean onlyIfAbsent) {
    int hash = spread(key);
      int binCount = 0;
      for (Node<V>[] tab = table;;) {
          Node<V> f; int n, i, fh; long fk; V fv;
        if (tab == null || (n = tab.length) == 0) {
          tab = initTable();
        }
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
          if (casTabAt(tab, i, new Node<V>(hash, key, value, null))) {
            break;                   // no lock when adding to empty bin
          }
        }
        else if ((fh = f.hash) == MOVED) {
          tab = helpTransfer(tab, f);
        }
        else if (onlyIfAbsent // check first node without acquiring lock
                 && fh == hash
                 && ((fk = f.key) == key)
                 && (fv = f.val) != null) {
          return fv;
        }
        else {
          V oldVal = null;
          synchronized (f) {
            if (tabAt(tab, i) == f) {
              if (fh >= 0) {
                binCount = 1;
                for (Node<V> e = f; ; ++binCount) {
                  long ek;
                  if (e.hash == hash &&
                      ((ek = e.key) == key)) {
                    oldVal = e.val;
                    if (!onlyIfAbsent) {
                      e.val = value;
                    }
                    break;
                  }
                  Node<V> pred = e;
                  if ((e = e.next) == null) {
                    pred.next = new Node<V>(hash, key, value, null);
                    break;
                  }
                }
              }
              else if (f instanceof TreeBin) {
                Node<V> p;
                binCount = 2;
                if ((p = ((TreeBin<V>)f).putTreeVal(hash, key,
                                                    value)) != null) {
                  oldVal = p.val;
                  if (!onlyIfAbsent) {
                    p.val = value;
                  }
                }
              }
              else if (f instanceof ReservationNode) {
                throw new IllegalStateException("Recursive update");
              }
            }
          }
          if (binCount != 0) {
            if (binCount >= TREEIFY_THRESHOLD) {
              treeifyBin(tab, i);
            }
            if (oldVal != null) {
              return oldVal;
            }
            break;
          }
        }
      }
      addCount(1L, binCount);
      return null;
  }

  /**
   * Removes the key (and its corresponding value) from this map.
   * This method does nothing if the key is not in the map.
   *
   * @param  key the key that needs to be removed
   * @return the previous value associated with {@code key}, or
   *         {@code null} if there was no mapping for {@code key}
   */
  @Override
  public V remove(long key) {
    return replaceNode(key, null, null);
  }

  /**
   * Implementation for the four public remove/replace methods:
   * Replaces node value with v, conditional upon match of cv if
   * non-null.  If resulting value is null, delete.
   */
  final V replaceNode(long key, V value, Object cv) {
    int hash = spread(key);
      for (Node<V>[] tab = table;;) {
          Node<V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0 ||
            (f = tabAt(tab, i = (n - 1) & hash)) == null) {
          break;
        }
        else if ((fh = f.hash) == MOVED) {
          tab = helpTransfer(tab, f);
        }
        else {
          V oldVal = null;
          boolean validated = false;
          synchronized (f) {
            if (tabAt(tab, i) == f) {
              if (fh >= 0) {
                validated = true;
                for (Node<V> e = f, pred = null; ; ) {
                  if (e.key == key) {
                    V ev = e.val;
                    if (cv == null || cv == ev ||
                        (ev != null && cv.equals(ev))) {
                      oldVal = ev;
                      if (value != null) {
                        e.val = value;
                      }
                      else if (pred != null) {
                        pred.next = e.next;
                      }
                      else {
                        setTabAt(tab, i, e.next);
                      }
                    }
                    break;
                  }
                  pred = e;
                  if ((e = e.next) == null) {
                    break;
                  }
                }
              }
              else if (f instanceof TreeBin) {
                validated = true;
                TreeBin<V> t = (TreeBin<V>)f;
                TreeNode<V> r, p;
                if ((r = t.root) != null &&
                    (p = r.findTreeNode(hash, key)) != null) {
                  V pv = p.val;
                  if (cv == null || cv == pv ||
                      (pv != null && cv.equals(pv))) {
                    oldVal = pv;
                    if (value != null) {
                      p.val = value;
                    }
                    else if (t.removeTreeNode(p)) {
                      setTabAt(tab, i, untreeify(t.first));
                    }
                  }
                }
              }
              else if (f instanceof ReservationNode) {
                throw new IllegalStateException("Recursive update");
              }
            }
          }
          if (validated) {
            if (oldVal != null) {
              if (value == null) {
                addCount(-1L, -1);
              }
              return oldVal;
            }
            break;
          }
        }
      }
      return null;
  }

  /**
   * Removes all of the mappings from this map.
   */
  public void clear() {
      long delta = 0L; // negative number of deletions
      int i = 0;
      Node<V>[] tab = table;
      while (tab != null && i < tab.length) {
          int fh;
          Node<V> f = tabAt(tab, i);
        if (f == null) {
          ++i;
        }
        else if ((fh = f.hash) == MOVED) {
          tab = helpTransfer(tab, f);
          i = 0; // restart
        }
        else {
          synchronized (f) {
            if (tabAt(tab, i) == f) {
              Node<V> p = (fh >= 0 ? f :
                           (f instanceof TreeBin) ?
                           ((TreeBin<V>)f).first : null);
              while (p != null) {
                --delta;
                p = p.next;
              }
              setTabAt(tab, i++, null);
            }
          }
        }
      }
    if (delta != 0L) {
      addCount(delta, -1);
    }
  }


  /**
   * Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  The collection
   * supports element removal, which removes the corresponding
   * mapping from this map, via the {@code Iterator.remove},
   * {@code Collection.remove}, {@code removeAll},
   * {@code retainAll}, and {@code clear} operations.  It does not
   * support the {@code add} or {@code addAll} operations.
   *
   * <p>The view's iterators and spliterators are
   * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
   *
   * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT}
   * and {@link Spliterator#NONNULL}.
   *
   * @return the collection view
   */
  public Collection<V> values() {
      ValuesView<V> vs;
      if ((vs = values) != null) return vs;
      return values = new ValuesView<V>(this);
  }

  /**
   * Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  The set supports element
   * removal, which removes the corresponding mapping from the map,
   * via the {@code Iterator.remove}, {@code Set.remove},
   * {@code removeAll}, {@code retainAll}, and {@code clear}
   * operations.
   *
   * <p>The view's iterators and spliterators are
   * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
   *
   * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
   * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
   *
   * @return the set view
   */
  @Override
  public Set<LongEntry<V>> entrySet() {
    EntrySetView<V> es;
    return (es = entrySet) != null ? es : (entrySet = new EntrySetView<>(this));
  }

  /**
   * Returns the hash code value for this {@link Map}, i.e.,
   * the sum of, for each key-value pair in the map,
   * {@code key.hashCode() ^ value.hashCode()}.
   *
   * @return the hash code value for this map
   */
  public int hashCode() {
      int h = 0;
      Node<V>[] t;
      if ((t = table) != null) {
          Traverser<V> it = new Traverser<V>(t, t.length, 0, t.length);
        for (Node<V> p; (p = it.advance()) != null; ) {
          h += p.hashCode();
        }
      }
      return h;
  }

  /**
   * Returns a string representation of this map.  The string
   * representation consists of a list of key-value mappings (in no
   * particular order) enclosed in braces ("{@code {}}").  Adjacent
   * mappings are separated by the characters {@code ", "} (comma
   * and space).  Each key-value mapping is rendered as the key
   * followed by an equals sign ("{@code =}") followed by the
   * associated value.
   *
   * @return a string representation of this map
   */
  public String toString() {
      Node<V>[] t;
      int f = (t = table) == null ? 0 : t.length;
      Traverser<V> it = new Traverser<V>(t, f, 0, f);
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      Node<V> p;
      if ((p = it.advance()) != null) {
          for (;;) {
              long k = p.key;
              V v = p.val;
              sb.append(k);
              sb.append('=');
              sb.append(v == this ? "(this Map)" : v);
            if ((p = it.advance()) == null) {
              break;
            }
              sb.append(',').append(' ');
          }
      }
      return sb.append('}').toString();
  }

  /**
   * Compares the specified object with this map for equality.
   * Returns {@code true} if the given object is a map with the same
   * mappings as this map.  This operation may return misleading
   * results if either map is concurrently modified during execution
   * of this method.
   *
   * @param o object to be compared for equality with this map
   * @return {@code true} if the specified object is equal to this map
   */
  @Override
  public boolean equals(Object o) {
    if (o != this) {
      if (!(o instanceof ConcurrentLongObjectMap)) {
        return false;
      }
      ConcurrentLongObjectMap<?> m = (ConcurrentLongObjectMap)o;
      Node<V>[] t;
      int f = (t = table) == null ? 0 : t.length;
      Traverser<V> it = new Traverser<>(t, f, 0, f);
      for (Node<V> p; (p = it.advance()) != null; ) {
        V val = p.val;
        Object v = m.get(p.key);
        if (v == null || (v != val && !v.equals(val))) {
          return false;
        }
      }
      for (LongEntry<?> e : m.entries()) {
        long mk = e.getKey();
        Object mv = e.getValue();
        Object v = get(mk);
        if (v == null || mv != v && !mv.equals(v)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Stripped-down version of helper class used in previous version,
   * declared for the sake of serialization compatibility.
   */
  static class Segment<V> extends ReentrantLock {
      final float loadFactor;
      Segment(float lf) { this.loadFactor = lf; }
  }



  // ConcurrentMap methods

  /**
   * {@inheritDoc}
   *
   * @return the previous value associated with the specified key,
   * or {@code null} if there was no mapping for the key
   */
  @Override
  public V putIfAbsent(long key, @NotNull V value) {
    return putVal(key, value, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remove(long key, @NotNull Object value) {
    return replaceNode(key, null, value) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean replace(long key, @NotNull V oldValue, @NotNull V newValue) {
    return replaceNode(key, newValue, oldValue) != null;
  }

  /**
   * {@inheritDoc}
   *
   * @return the previous value associated with the specified key,
   * or {@code null} if there was no mapping for the key
   */
  @Override
  public V replace(long key, @NotNull V value) {
    return replaceNode(key, value, null);
  }

  // Overrides of JDK8+ Map extension method defaults

  /**
   * Returns the value to which the specified key is mapped, or the
   * given default value if this map contains no mapping for the
   * key.
   *
   * @param key the key whose associated value is to be returned
   * @param defaultValue the value to return if this map contains
   * no mapping for the given key
   * @return the mapping for the key, if present; else the default value
   */
  @Override
  public V getOrDefault(long key, V defaultValue) {
    V v;
    return (v = get(key)) == null ? defaultValue : v;
  }

  boolean removeValueIf(Predicate<? super V> function) {
      if (function == null) throw new NullPointerException();
      Node<V>[] t;
      boolean removed = false;
      if ((t = table) != null) {
          Traverser<V> it = new Traverser<V>(t, t.length, 0, t.length);
          for (Node<V> p; (p = it.advance()) != null; ) {
              long k = p.key;
              V v = p.val;
            if (function.test(v) && replaceNode(k, null, v) != null) {
              removed = true;
            }
          }
      }
      return removed;
  }

  // Hashtable legacy methods

  /**
   * Tests if some key maps into the specified value in this table.
   *
   * <p>Note that this method is identical in functionality to
   * {@link #containsValue(Object)}, and exists solely to ensure
   * full compatibility with class {@link java.util.Hashtable},
   * which supported this method prior to introduction of the
   * Java Collections Framework.
   *
   * @param  value a value to search for
   * @return {@code true} if and only if some key maps to the
   *         {@code value} argument in this table as
   *         determined by the {@code equals} method;
   *         {@code false} otherwise
   * @throws NullPointerException if the specified value is null
   */
  public boolean contains(Object value) {
      return containsValue(value);
  }

  /**
   * Returns an enumeration of the keys in this table.
   *
   * @return an enumeration of the keys in this table
   */
  @Override
  public long @NotNull [] keys() {
    Object[] entries = new EntrySetView<>(this).toArray();
    long[] result = new long[entries.length];
    for (int i = 0; i < entries.length; i++) {
      LongEntry<V> entry = (LongEntry<V>)entries[i];
      result[i] = entry.getKey();
    }
    return result;
  }

  /**
   * Returns an enumeration of the values in this table.
   *
   * @return an enumeration of the values in this table
   * @see #values()
   */
  @Override
  @NotNull
  public Iterator<V> elements() {
    Node<V>[] t;
    int f = (t = table) == null ? 0 : t.length;
    return new ValueIterator<>(t, f, 0, f, this);
  }

  // ConcurrentHashMap-only methods

  /**
   * Returns the number of mappings. This method should be used
   * instead of {@link #size} because a ConcurrentHashMap may
   * contain more mappings than can be represented as an int. The
   * value returned is an estimate; the actual count may differ if
   * there are concurrent insertions or removals.
   *
   * @return the number of mappings
   * @since 1.8
   */
  public long mappingCount() {
      long n = sumCount();
      return (n < 0L) ? 0L : n; // ignore transient negative values
  }



  /* ---------------- Special Nodes -------------- */

  /**
   * A node inserted at head of bins during transfer operations.
   */
  static final class ForwardingNode<V> extends Node<V> {
      final Node<V>[] nextTable;
      ForwardingNode(Node<V>[] tab) {
          super(MOVED, 0, null, null);
          this.nextTable = tab;
      }

    @Override
    Node<V> find(int h, long k) {
      // loop to avoid arbitrarily deep recursion on forwarding nodes
      outer:
      for (Node<V>[] tab = nextTable; ; ) {
        Node<V> e;
        int n;
        if (tab == null || (n = tab.length) == 0 ||
            (e = tabAt(tab, (n - 1) & h)) == null) {
          return null;
        }
        for (; ; ) {
          if ((e.key == k)) {
            return e;
          }
          if (e.hash < 0) {
            if (e instanceof ForwardingNode) {
              tab = ((ForwardingNode<V>)e).nextTable;
              continue outer;
            }
            else {
              return e.find(h, k);
            }
          }
          if ((e = e.next) == null) {
            return null;
          }
        }
      }
    }
  }

  /**
   * A place-holder node used in computeIfAbsent and compute.
   */
  static final class ReservationNode<V> extends Node<V> {
      ReservationNode() {
          super(RESERVED, 0, null, null);
      }

      Node<V> find(int h, Object k) {
          return null;
      }
  }

  /* ---------------- Table Initialization and Resizing -------------- */

  /**
   * Returns the stamp bits for resizing a table of size n.
   * Must be negative when shifted left by RESIZE_STAMP_SHIFT.
   */
  static final int resizeStamp(int n) {
      return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
  }

  /**
   * Initializes table, using the size recorded in sizeCtl.
   */
  private final Node<V>[] initTable() {
      Node<V>[] tab; int sc;
      while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0) {
          Thread.yield(); // lost initialization race; just spin
        }
        else if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, -1)) {
          try {
            if ((tab = table) == null || tab.length == 0) {
              int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
              @SuppressWarnings("unchecked")
              Node<V>[] nt = (Node<V>[])new Node<?>[n];
              table = tab = nt;
              sc = n - (n >>> 2);
            }
          }
          finally {
            sizeCtl = sc;
          }
          break;
        }
      }
      return tab;
  }

  /**
   * Adds to count, and if table is too small and not already
   * resizing, initiates transfer. If already resizing, helps
   * perform transfer if work is available.  Rechecks occupancy
   * after a transfer to see if another resize is already needed
   * because resizings are lagging additions.
   *
   * @param x the count to add
   * @param check if <0, don't check resize, if <= 1 only check if uncontended
   */
  private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
      if ((as = counterCells) != null ||
          !Unsafe.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell a; long v; int m;
          boolean uncontended = true;
          if (as == null || (m = as.length - 1) < 0 ||
              (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
              !(uncontended =
                  Unsafe.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
              fullAddCount(x, uncontended);
              return;
          }
        if (check <= 1) {
          return;
        }
          s = sumCount();
      }
      if (check >= 0) {
          Node<V>[] tab, nt; int n, sc;
          while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
                 (n = tab.length) < MAXIMUM_CAPACITY) {
              int rs = resizeStamp(n) << RESIZE_STAMP_SHIFT;
              if (sc < 0) {
                if (sc == rs + MAX_RESIZERS || sc == rs + 1 ||
                    (nt = nextTable) == null || transferIndex <= 0) {
                  break;
                }
                if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                  transfer(tab, nt);
                }
              }
              else if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, rs + 2)) {
                transfer(tab, null);
              }
              s = sumCount();
          }
      }
  }

  /**
   * Helps transfer if a resize is in progress.
   */
  final Node<V>[] helpTransfer(Node<V>[] tab, Node<V> f) {
      Node<V>[] nextTab; int sc;
      if (tab != null && (f instanceof ForwardingNode) &&
          (nextTab = ((ForwardingNode<V>)f).nextTable) != null) {
          int rs = resizeStamp(tab.length) << RESIZE_STAMP_SHIFT;
          while (nextTab == nextTable && table == tab &&
                 (sc = sizeCtl) < 0) {
            if (sc == rs + MAX_RESIZERS || sc == rs + 1 ||
                transferIndex <= 0) {
              break;
            }
              if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                  transfer(tab, nextTab);
                  break;
              }
          }
          return nextTab;
      }
      return table;
  }

  /**
   * Tries to presize table to accommodate the given number of elements.
   *
   * @param size number of elements (doesn't need to be perfectly accurate)
   */
  private final void tryPresize(int size) {
      int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
          tableSizeFor(size + (size >>> 1) + 1);
      int sc;
      while ((sc = sizeCtl) >= 0) {
          Node<V>[] tab = table; int n;
          if (tab == null || (n = tab.length) == 0) {
              n = (sc > c) ? sc : c;
              if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                  try {
                      if (table == tab) {
                          @SuppressWarnings("unchecked")
                          Node<V>[] nt = (Node<V>[])new Node<?>[n];
                          table = nt;
                          sc = n - (n >>> 2);
                      }
                  } finally {
                      sizeCtl = sc;
                  }
              }
          }
          else if (c <= sc || n >= MAXIMUM_CAPACITY) {
            break;
          }
          else if (tab == table) {
            int rs = resizeStamp(n);
            if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2)) {
              transfer(tab, null);
            }
          }
      }
  }

  /**
   * Moves and/or copies the nodes in each bin to new table. See
   * above for explanation.
   */
  private final void transfer(Node<V>[] tab, Node<V>[] nextTab) {
      int n = tab.length, stride;
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE) {
      stride = MIN_TRANSFER_STRIDE; // subdivide range
    }
      if (nextTab == null) {            // initiating
          try {
              @SuppressWarnings("unchecked")
              Node<V>[] nt = (Node<V>[])new Node<?>[n << 1];
              nextTab = nt;
          } catch (Throwable ex) {      // try to cope with OOME
              sizeCtl = Integer.MAX_VALUE;
              return;
          }
          nextTable = nextTab;
          transferIndex = n;
      }
      int nextn = nextTab.length;
      ForwardingNode<V> fwd = new ForwardingNode<V>(nextTab);
      boolean advance = true;
      boolean finishing = false; // to ensure sweep before committing nextTab
      for (int i = 0, bound = 0;;) {
          Node<V> f; int fh;
          while (advance) {
              int nextIndex, nextBound;
            if (--i >= bound || finishing) {
              advance = false;
            }
            else if ((nextIndex = transferIndex) <= 0) {
              i = -1;
              advance = false;
            }
            else if (Unsafe.compareAndSwapInt
                      (this, TRANSFERINDEX, nextIndex,
                       nextBound = (nextIndex > stride ?
                                    nextIndex - stride : 0))) {
              bound = nextBound;
              i = nextIndex - 1;
              advance = false;
            }
          }
          if (i < 0 || i >= n || i + n >= nextn) {
              int sc;
              if (finishing) {
                  nextTable = null;
                  table = nextTab;
                  sizeCtl = (n << 1) - (n >>> 1);
                  return;
              }
              sc = sizeCtl;
              if (Unsafe.compareAndSwapInt(this, SIZECTL, sc, sc - 1)) {
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) {
                  return;
                }
                  finishing = advance = true;
                  i = n; // recheck before commit
              }
          }
          else if ((f = tabAt(tab, i)) == null) {
            advance = casTabAt(tab, i, fwd);
          }
          else if ((fh = f.hash) == MOVED) {
            advance = true; // already processed
          }
          else {
            synchronized (f) {
              if (tabAt(tab, i) == f) {
                Node<V> ln, hn;
                if (fh >= 0) {
                  int runBit = fh & n;
                  Node<V> lastRun = f;
                  for (Node<V> p = f.next; p != null; p = p.next) {
                    int b = p.hash & n;
                    if (b != runBit) {
                      runBit = b;
                      lastRun = p;
                    }
                  }
                  if (runBit == 0) {
                    ln = lastRun;
                    hn = null;
                  }
                  else {
                    hn = lastRun;
                    ln = null;
                  }
                  for (Node<V> p = f; p != lastRun; p = p.next) {
                    int ph = p.hash;
                    long pk = p.key;
                    V pv = p.val;
                    if ((ph & n) == 0) {
                      ln = new Node<V>(ph, pk, pv, ln);
                    }
                    else {
                      hn = new Node<V>(ph, pk, pv, hn);
                    }
                  }
                  setTabAt(nextTab, i, ln);
                  setTabAt(nextTab, i + n, hn);
                  setTabAt(tab, i, fwd);
                  advance = true;
                }
                else if (f instanceof TreeBin) {
                  TreeBin<V> t = (TreeBin<V>)f;
                  TreeNode<V> lo = null, loTail = null;
                  TreeNode<V> hi = null, hiTail = null;
                  int lc = 0, hc = 0;
                  for (Node<V> e = t.first; e != null; e = e.next) {
                    int h = e.hash;
                    TreeNode<V> p = new TreeNode<V>
                      (h, e.key, e.val, null, null);
                    if ((h & n) == 0) {
                      if ((p.prev = loTail) == null) {
                        lo = p;
                      }
                      else {
                        loTail.next = p;
                      }
                      loTail = p;
                      ++lc;
                    }
                    else {
                      if ((p.prev = hiTail) == null) {
                        hi = p;
                      }
                      else {
                        hiTail.next = p;
                      }
                      hiTail = p;
                      ++hc;
                    }
                  }
                  ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                       (hc != 0) ? new TreeBin<V>(lo) : t;
                  hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                       (lc != 0) ? new TreeBin<V>(hi) : t;
                  setTabAt(nextTab, i, ln);
                  setTabAt(nextTab, i + n, hn);
                  setTabAt(tab, i, fwd);
                  advance = true;
                }
                else if (f instanceof ReservationNode) {
                  throw new IllegalStateException("Recursive update");
                }
              }
            }
          }
      }
  }

  /* ---------------- Counter support -------------- */


  static final class CounterCell {
      // Padding fields to avoid contention
      volatile long p0, p1, p2, p3, p4, p5, p6;
      volatile long value;
      // Padding fields to avoid contention
      volatile long q0, q1, q2, q3, q4, q5, q6;
      CounterCell(long x) { value = x; }
  }
  final long sumCount() {
      CounterCell[] cs = counterCells;
      long sum = baseCount;
      if (cs != null) {
        for (CounterCell c : cs) {
          if (c != null) {
            sum += c.value;
          }
        }
      }
      return sum;
  }

  // See LongAdder version for explanation
  private final void fullAddCount(long x, boolean wasUncontended) {
      int h;
      if ((h = ThreadLocalRandom.getProbe()) == 0) {
          ThreadLocalRandom.localInit();      // force initialization
          h = ThreadLocalRandom.getProbe();
          wasUncontended = true;
      }
      boolean collide = false;                // True if last slot nonempty
      for (;;) {
          CounterCell[] cs; CounterCell c; int n; long v;
          if ((cs = counterCells) != null && (n = cs.length) > 0) {
              if ((c = cs[(n - 1) & h]) == null) {
                  if (cellsBusy == 0) {            // Try to attach new Cell
                    CounterCell r = new CounterCell(x); // Optimistic create
                      if (cellsBusy == 0 &&
                          Unsafe.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                          boolean created = false;
                          try {               // Recheck under lock
                            CounterCell[] rs; int m, j;
                              if ((rs = counterCells) != null &&
                                  (m = rs.length) > 0 &&
                                  rs[j = (m - 1) & h] == null) {
                                  rs[j] = r;
                                  created = true;
                              }
                          } finally {
                              cellsBusy = 0;
                          }
                        if (created) {
                          break;
                        }
                          continue;           // Slot is now non-empty
                      }
                  }
                  collide = false;
              }
              else if (!wasUncontended)       // CAS already known to fail
              {
                wasUncontended = true;      // Continue after rehash
              }
              else if (Unsafe.compareAndSwapLong(c, CELLVALUE, v = c.value, v + x)) {
                break;
              }
              else if (counterCells != cs || n >= NCPU) {
                collide = false;            // At max size or stale
              }
              else if (!collide) {
                collide = true;
              }
              else if (cellsBusy == 0 &&
                       Unsafe.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                try {
                  if (counterCells == cs) // Expand table unless stale
                  {
                    counterCells = Arrays.copyOf(cs, n << 1);
                  }
                }
                finally {
                  cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
              }
              h = ThreadLocalRandom.advanceProbe(h);
          }
          else if (cellsBusy == 0 && counterCells == cs &&
                   Unsafe.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
              boolean init = false;
              try {                           // Initialize table
                  if (counterCells == cs) {
                    CounterCell[] rs = new CounterCell[2];
                      rs[h & 1] = new CounterCell(x);
                      counterCells = rs;
                      init = true;
                  }
              } finally {
                  cellsBusy = 0;
              }
            if (init) {
              break;
            }
          }
          else if (Unsafe.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x)) {
            break;                          // Fall back on using base
          }
      }
  }

  /* ---------------- Conversion from/to TreeBins -------------- */

  /**
   * Replaces all linked nodes in bin at given index unless table is
   * too small, in which case resizes instead.
   */
  private final void treeifyBin(Node<V>[] tab, int index) {
      Node<V> b; int n;
      if (tab != null) {
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY) {
          tryPresize(n << 1);
        }
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
          synchronized (b) {
            if (tabAt(tab, index) == b) {
              TreeNode<V> hd = null, tl = null;
              for (Node<V> e = b; e != null; e = e.next) {
                TreeNode<V> p =
                  new TreeNode<V>(e.hash, e.key, e.val,
                                  null, null);
                if ((p.prev = tl) == null) {
                  hd = p;
                }
                else {
                  tl.next = p;
                }
                tl = p;
              }
              setTabAt(tab, index, new TreeBin<V>(hd));
            }
          }
        }
      }
  }

  /**
   * Returns a list of non-TreeNodes replacing those in given list.
   */
  Node<V> untreeify(Node<V> b) {
      Node<V> hd = null, tl = null;
      for (Node<V> q = b; q != null; q = q.next) {
          Node<V> p = new Node<V>(q.hash, q.key, q.val, null);
        if (tl == null) {
          hd = p;
        }
        else {
          tl.next = p;
        }
          tl = p;
      }
      return hd;
  }

  /* ---------------- TreeNodes -------------- */

  /**
   * Nodes for use in TreeBins.
   */
  static final class TreeNode<V> extends Node<V> {
      TreeNode<V> parent;  // red-black tree links
      TreeNode<V> left;
      TreeNode<V> right;
      TreeNode<V> prev;    // needed to unlink next upon deletion
      boolean red;

      TreeNode(int hash, long key, V val, Node<V> next,
               TreeNode<V> parent) {
          super(hash, key, val, next);
          this.parent = parent;
      }

      Node<V> find(int h, long k) {
          return findTreeNode(h, k);
      }

      /**
       * Returns the TreeNode (or null if not found) for the given key
       * starting at given root.
       */
      final TreeNode<V> findTreeNode(int h, long k) {
        TreeNode<V> p = this;
        do {
          int ph;
          TreeNode<V> q;
          TreeNode<V> pl = p.left;
          TreeNode<V> pr = p.right;
          if ((ph = p.hash) > h) {
            p = pl;
          }
          else if (ph < h) {
            p = pr;
          }
          else if (p.key == k) {
            return p;
          }
          else if (pl == null) {
            p = pr;
          }
          else if (pr == null) {
            p = pl;
          }
          else if ((q = pr.findTreeNode(h, k)) != null) {
            return q;
          }
          else {
            p = pl;
          }
        }
        while (p != null);
        return null;
      }
  }

  /* ---------------- TreeBins -------------- */

  /**
   * TreeNodes used at the heads of bins. TreeBins do not hold user
   * keys or values, but instead point to list of TreeNodes and
   * their root. They also maintain a parasitic read-write lock
   * forcing writers (who hold bin lock) to wait for readers (who do
   * not) to complete before tree restructuring operations.
   */
  static final class TreeBin<V> extends Node<V> {
      TreeNode<V> root;
      volatile TreeNode<V> first;
      volatile Thread waiter;
      volatile int lockState;
      // values for lockState
      static final int WRITER = 1; // set while holding write lock
      static final int WAITER = 2; // set when waiting for write lock
      static final int READER = 4; // increment value for setting read lock

      /**
       * Creates bin with initial set of nodes headed by b.
       */
      TreeBin(TreeNode<V> b) {
          super(TREEBIN, 0, null, null);
          this.first = b;
          TreeNode<V> r = null;
          for (TreeNode<V> x = b, next; x != null; x = next) {
              next = (TreeNode<V>)x.next;
              x.left = x.right = null;
              if (r == null) {
                  x.parent = null;
                  x.red = false;
                  r = x;
              }
              else {
                  int h = x.hash;
                  for (TreeNode<V> p = r;;) {
                      int dir, ph;
                    if ((ph = p.hash) > h) {
                      dir = -1;
                    }
                    else if (ph < h) {
                      dir = 1;
                    }
                    else {
                      dir = 0;
                    }
                      TreeNode<V> xp = p;
                      if ((p = (dir <= 0) ? p.left : p.right) == null) {
                          x.parent = xp;
                        if (dir <= 0) {
                          xp.left = x;
                        }
                        else {
                          xp.right = x;
                        }
                          r = balanceInsertion(r, x);
                          break;
                      }
                  }
              }
          }
          this.root = r;
          assert checkInvariants(root);
      }

      /**
       * Acquires write lock for tree restructuring.
       */
      private final void lockRoot() {
        if (!Unsafe.compareAndSwapInt(this, LOCKSTATE, 0, WRITER)) {
          contendedLock(); // offload to separate method
        }
      }

      /**
       * Releases write lock for tree restructuring.
       */
      private final void unlockRoot() {
          lockState = 0;
      }

      /**
       * Possibly blocks awaiting root lock.
       */
      private final void contendedLock() {
          boolean waiting = false;
          for (int s;;) {
              if (((s = lockState) & ~WAITER) == 0) {
                  if (Unsafe.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                    if (waiting) {
                      waiter = null;
                    }
                      return;
                  }
              }
              else if ((s & WAITER) == 0) {
                  if (Unsafe.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                      waiting = true;
                      waiter = Thread.currentThread();
                  }
              }
              else if (waiting) {
                LockSupport.park(this);
              }
          }
      }

      /**
       * Returns matching node or null if none. Tries to search
       * using tree comparisons from root, but continues linear
       * search when lock not available.
       */
      @Override
      Node<V> find(int h, long k) {
        for (Node<V> e = first; e != null; ) {
          int s;
          if (((s = lockState) & (WAITER | WRITER)) != 0) {
            if ((e.key == k)) {
              return e;
            }
            e = e.next;
          }
          else if (Unsafe.compareAndSwapInt(this, LOCKSTATE, s,
                                               s + READER)) {
            TreeNode<V> r;
            TreeNode<V> p;
            try {
              p = ((r = root) == null ? null :
                   r.findTreeNode(h, k));
            }
            finally {
              Thread w;
              if (getAndAddInt(this, LOCKSTATE, -READER) ==
                              (READER | WAITER) && (w = waiter) != null) {
                LockSupport.unpark(w);
              }
            }
            return p;
          }
        }
        return null;
      }

    private static int getAndAddInt(Object object, long offset, int v) {
      try {
        return Unsafe.getAndAddInt(object, offset, v);
      }
      catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
      /**
       * Finds or adds a node.
       * @return null if added
       */
      TreeNode<V> putTreeVal(int h, long k, V v) {
        boolean searched = false;
        for (TreeNode<V> p = root; ; ) {
          int dir, ph;
          if (p == null) {
            first = root = new TreeNode<>(h, k, v, null, null);
            break;
          }
          else if ((ph = p.hash) > h) {
            dir = -1;
          }
          else if (ph < h) {
            dir = 1;
          }
          else if (p.key == k) {
            return p;
          }
          else {
            if (!searched) {
              TreeNode<V> q, ch;
              searched = true;
              if (((ch = p.left) != null &&
                   (q = ch.findTreeNode(h, k)) != null) ||
                  ((ch = p.right) != null &&
                   (q = ch.findTreeNode(h, k)) != null)) {
                return q;
              }
            }
            dir = 0;
          }

          TreeNode<V> xp = p;
          if ((p = (dir <= 0) ? p.left : p.right) == null) {
            TreeNode<V> x, f = first;
            first = x = new TreeNode<>(h, k, v, f, xp);
            if (f != null) {
              f.prev = x;
            }
            if (dir <= 0) {
              xp.left = x;
            }
            else {
              xp.right = x;
            }
            if (!xp.red) {
              x.red = true;
            }
            else {
              lockRoot();
              try {
                root = balanceInsertion(root, x);
              }
              finally {
                unlockRoot();
              }
            }
            break;
          }
        }
        assert checkInvariants(root);
        return null;
      }

      /**
       * Removes the given node, that must be present before this
       * call.  This is messier than typical red-black deletion code
       * because we cannot swap the contents of an interior node
       * with a leaf successor that is pinned by "next" pointers
       * that are accessible independently of lock. So instead we
       * swap the tree linkages.
       *
       * @return true if now too small, so should be untreeified
       */
      final boolean removeTreeNode(TreeNode<V> p) {
          TreeNode<V> next = (TreeNode<V>)p.next;
          TreeNode<V> pred = p.prev;  // unlink traversal pointers
          TreeNode<V> r, rl;
        if (pred == null) {
          first = next;
        }
        else {
          pred.next = next;
        }
        if (next != null) {
          next.prev = pred;
        }
          if (first == null) {
              root = null;
              return true;
          }
        if ((r = root) == null || r.right == null || // too small
            (rl = r.left) == null || rl.left == null) {
          return true;
        }
          lockRoot();
          try {
              TreeNode<V> replacement;
              TreeNode<V> pl = p.left;
              TreeNode<V> pr = p.right;
              if (pl != null && pr != null) {
                  TreeNode<V> s = pr, sl;
                while ((sl = s.left) != null) // find successor
                {
                  s = sl;
                }
                  boolean c = s.red; s.red = p.red; p.red = c; // swap colors
                  TreeNode<V> sr = s.right;
                  TreeNode<V> pp = p.parent;
                  if (s == pr) { // p was s's direct parent
                      p.parent = s;
                      s.right = p;
                  }
                  else {
                      TreeNode<V> sp = s.parent;
                      if ((p.parent = sp) != null) {
                        if (s == sp.left) {
                          sp.left = p;
                        }
                        else {
                          sp.right = p;
                        }
                      }
                    if ((s.right = pr) != null) {
                      pr.parent = s;
                    }
                  }
                  p.left = null;
                if ((p.right = sr) != null) {
                  sr.parent = p;
                }
                if ((s.left = pl) != null) {
                  pl.parent = s;
                }
                if ((s.parent = pp) == null) {
                  r = s;
                }
                else if (p == pp.left) {
                  pp.left = s;
                }
                else {
                  pp.right = s;
                }
                if (sr != null) {
                  replacement = sr;
                }
                else {
                  replacement = p;
                }
              }
              else if (pl != null) {
                replacement = pl;
              }
              else if (pr != null) {
                replacement = pr;
              }
              else {
                replacement = p;
              }
              if (replacement != p) {
                  TreeNode<V> pp = replacement.parent = p.parent;
                if (pp == null) {
                  r = replacement;
                }
                else if (p == pp.left) {
                  pp.left = replacement;
                }
                else {
                  pp.right = replacement;
                }
                  p.left = p.right = p.parent = null;
              }

              root = (p.red) ? r : balanceDeletion(r, replacement);

              if (p == replacement) {  // detach pointers
                  TreeNode<V> pp;
                  if ((pp = p.parent) != null) {
                    if (p == pp.left) {
                      pp.left = null;
                    }
                    else if (p == pp.right) {
                      pp.right = null;
                    }
                      p.parent = null;
                  }
              }
          } finally {
              unlockRoot();
          }
          assert checkInvariants(root);
          return false;
      }

      /* ------------------------------------------------------------ */
      // Red-black tree methods, all adapted from CLR

      static <V> TreeNode<V> rotateLeft(TreeNode<V> root,
                                            TreeNode<V> p) {
          TreeNode<V> r, pp, rl;
          if (p != null && (r = p.right) != null) {
            if ((rl = p.right = r.left) != null) {
              rl.parent = p;
            }
            if ((pp = r.parent = p.parent) == null) {
              (root = r).red = false;
            }
            else if (pp.left == p) {
              pp.left = r;
            }
            else {
              pp.right = r;
            }
              r.left = p;
              p.parent = r;
          }
          return root;
      }

      static <V> TreeNode<V> rotateRight(TreeNode<V> root,
                                             TreeNode<V> p) {
          TreeNode<V> l, pp, lr;
          if (p != null && (l = p.left) != null) {
            if ((lr = p.left = l.right) != null) {
              lr.parent = p;
            }
            if ((pp = l.parent = p.parent) == null) {
              (root = l).red = false;
            }
            else if (pp.right == p) {
              pp.right = l;
            }
            else {
              pp.left = l;
            }
              l.right = p;
              p.parent = l;
          }
          return root;
      }

      static <V> TreeNode<V> balanceInsertion(TreeNode<V> root,
                                                  TreeNode<V> x) {
          x.red = true;
          for (TreeNode<V> xp, xpp, xppl, xppr;;) {
              if ((xp = x.parent) == null) {
                  x.red = false;
                  return x;
              }
              else if (!xp.red || (xpp = xp.parent) == null) {
                return root;
              }
              if (xp == (xppl = xpp.left)) {
                  if ((xppr = xpp.right) != null && xppr.red) {
                      xppr.red = false;
                      xp.red = false;
                      xpp.red = true;
                      x = xpp;
                  }
                  else {
                      if (x == xp.right) {
                          root = rotateLeft(root, x = xp);
                          xpp = (xp = x.parent) == null ? null : xp.parent;
                      }
                      if (xp != null) {
                          xp.red = false;
                          if (xpp != null) {
                              xpp.red = true;
                              root = rotateRight(root, xpp);
                          }
                      }
                  }
              }
              else {
                  if (xppl != null && xppl.red) {
                      xppl.red = false;
                      xp.red = false;
                      xpp.red = true;
                      x = xpp;
                  }
                  else {
                      if (x == xp.left) {
                          root = rotateRight(root, x = xp);
                          xpp = (xp = x.parent) == null ? null : xp.parent;
                      }
                      if (xp != null) {
                          xp.red = false;
                          if (xpp != null) {
                              xpp.red = true;
                              root = rotateLeft(root, xpp);
                          }
                      }
                  }
              }
          }
      }

      static <V> TreeNode<V> balanceDeletion(TreeNode<V> root,
                                                 TreeNode<V> x) {
          for (TreeNode<V> xp, xpl, xpr;;) {
            if (x == null || x == root) {
              return root;
            }
            else if ((xp = x.parent) == null) {
              x.red = false;
              return x;
            }
            else if (x.red) {
              x.red = false;
              return root;
            }
            else if ((xpl = xp.left) == x) {
              if ((xpr = xp.right) != null && xpr.red) {
                xpr.red = false;
                xp.red = true;
                root = rotateLeft(root, xp);
                xpr = (xp = x.parent) == null ? null : xp.right;
              }
              if (xpr == null) {
                x = xp;
              }
              else {
                TreeNode<V> sl = xpr.left, sr = xpr.right;
                if ((sr == null || !sr.red) &&
                    (sl == null || !sl.red)) {
                  xpr.red = true;
                  x = xp;
                }
                else {
                  if (sr == null || !sr.red) {
                    if (sl != null) {
                      sl.red = false;
                    }
                    xpr.red = true;
                    root = rotateRight(root, xpr);
                    xpr = (xp = x.parent) == null ?
                          null : xp.right;
                  }
                  if (xpr != null) {
                    xpr.red = (xp == null) ? false : xp.red;
                    if ((sr = xpr.right) != null) {
                      sr.red = false;
                    }
                  }
                  if (xp != null) {
                    xp.red = false;
                    root = rotateLeft(root, xp);
                  }
                  x = root;
                }
              }
            }
            else { // symmetric
              if (xpl != null && xpl.red) {
                xpl.red = false;
                xp.red = true;
                root = rotateRight(root, xp);
                xpl = (xp = x.parent) == null ? null : xp.left;
              }
              if (xpl == null) {
                x = xp;
              }
              else {
                TreeNode<V> sl = xpl.left, sr = xpl.right;
                if ((sl == null || !sl.red) &&
                    (sr == null || !sr.red)) {
                  xpl.red = true;
                  x = xp;
                }
                else {
                  if (sl == null || !sl.red) {
                    if (sr != null) {
                      sr.red = false;
                    }
                    xpl.red = true;
                    root = rotateLeft(root, xpl);
                    xpl = (xp = x.parent) == null ?
                          null : xp.left;
                  }
                  if (xpl != null) {
                    xpl.red = (xp == null) ? false : xp.red;
                    if ((sl = xpl.left) != null) {
                      sl.red = false;
                    }
                  }
                  if (xp != null) {
                    xp.red = false;
                    root = rotateRight(root, xp);
                  }
                  x = root;
                }
              }
            }
          }
      }

      /**
       * Checks invariants recursively for the tree of Nodes rooted at t.
       */
      static <V> boolean checkInvariants(TreeNode<V> t) {
          TreeNode<V> tp = t.parent, tl = t.left, tr = t.right,
              tb = t.prev, tn = (TreeNode<V>)t.next;
        if (tb != null && tb.next != t) {
          return false;
        }
        if (tn != null && tn.prev != t) {
          return false;
        }
        if (tp != null && t != tp.left && t != tp.right) {
          return false;
        }
        if (tl != null && (tl.parent != t || tl.hash > t.hash)) {
          return false;
        }
        if (tr != null && (tr.parent != t || tr.hash < t.hash)) {
          return false;
        }
        if (t.red && tl != null && tl.red && tr != null && tr.red) {
          return false;
        }
        if (tl != null && !checkInvariants(tl)) {
          return false;
        }
        if (tr != null && !checkInvariants(tr)) {
          return false;
        }
          return true;
      }

    private static final long LOCKSTATE;

    static {
      try {
        Class<?> k = TreeBin.class;
        Field field = k.getDeclaredField("lockState");
        LOCKSTATE = Unsafe.objectFieldOffset(field);
      }
      catch (Throwable t) {
        throw new Error(t);
      }
    }
  }

  /* ----------------Table Traversal -------------- */

  /**
   * Records the table, its length, and current traversal index for a
   * traverser that must process a region of a forwarded table before
   * proceeding with current table.
   */
  static final class TableStack<V> {
      int length;
      int index;
      Node<V>[] tab;
      TableStack<V> next;
  }

  /**
   * Encapsulates traversal for methods such as containsValue; also
   * serves as a base class for other iterators and spliterators.
   *
   * Method advance visits once each still-valid node that was
   * reachable upon iterator construction. It might miss some that
   * were added to a bin after the bin was visited, which is OK wrt
   * consistency guarantees. Maintaining this property in the face
   * of possible ongoing resizes requires a fair amount of
   * bookkeeping state that is difficult to optimize away amidst
   * volatile accesses.  Even so, traversal maintains reasonable
   * throughput.
   *
   * Normally, iteration proceeds bin-by-bin traversing lists.
   * However, if the table has been resized, then all future steps
   * must traverse both the bin at the current index as well as at
   * (index + baseSize); and so on for further resizings. To
   * paranoically cope with potential sharing by users of iterators
   * across threads, iteration terminates if a bounds checks fails
   * for a table read.
   */
  static class Traverser<V> {
      Node<V>[] tab;        // current table; updated if resized
      Node<V> next;         // the next entry to use
      TableStack<V> stack, spare; // to save/restore on ForwardingNodes
      int index;              // index of bin to use next
      int baseIndex;          // current index of initial table
      int baseLimit;          // index bound for initial table
      final int baseSize;     // initial table size

      Traverser(Node<V>[] tab, int size, int index, int limit) {
          this.tab = tab;
          this.baseSize = size;
          this.baseIndex = this.index = index;
          this.baseLimit = limit;
          this.next = null;
      }

      /**
       * Advances if possible, returning next valid node, or null if none.
       */
      final Node<V> advance() {
          Node<V> e;
        if ((e = next) != null) {
          e = e.next;
        }
          for (;;) {
              Node<V>[] t; int i, n;  // must use locals in checks
            if (e != null) {
              return next = e;
            }
            if (baseIndex >= baseLimit || (t = tab) == null ||
                (n = t.length) <= (i = index) || i < 0) {
              return next = null;
            }
              if ((e = tabAt(t, i)) != null && e.hash < 0) {
                  if (e instanceof ForwardingNode) {
                      tab = ((ForwardingNode<V>)e).nextTable;
                      e = null;
                      pushState(t, i, n);
                      continue;
                  }
                  else if (e instanceof TreeBin) {
                    e = ((TreeBin<V>)e).first;
                  }
                  else {
                    e = null;
                  }
              }
            if (stack != null) {
              recoverState(n);
            }
            else if ((index = i + baseSize) >= n) {
              index = ++baseIndex; // visit upper slots if present
            }
          }
      }

      /**
       * Saves traversal state upon encountering a forwarding node.
       */
      private void pushState(Node<V>[] t, int i, int n) {
          TableStack<V> s = spare;  // reuse if possible
        if (s != null) {
          spare = s.next;
        }
        else {
          s = new TableStack<V>();
        }
          s.tab = t;
          s.length = n;
          s.index = i;
          s.next = stack;
          stack = s;
      }

      /**
       * Possibly pops traversal state.
       *
       * @param n length of current table
       */
      private void recoverState(int n) {
          TableStack<V> s; int len;
          while ((s = stack) != null && (index += (len = s.length)) >= n) {
              n = len;
              index = s.index;
              tab = s.tab;
              s.tab = null;
              TableStack<V> next = s.next;
              s.next = spare; // save for reuse
              stack = next;
              spare = s;
          }
        if (s == null && (index += baseSize) >= n) {
          index = ++baseIndex;
        }
      }
  }

  /**
   * Base of key, value, and entry Iterators. Adds fields to
   * Traverser to support iterator.remove.
   */
  static class BaseIterator<V> extends Traverser<V> {
      final ConcurrentLongObjectHashMap<V> map;
      Node<V> lastReturned;
      BaseIterator(Node<V>[] tab, int size, int index, int limit,
                   ConcurrentLongObjectHashMap<V> map) {
          super(tab, size, index, limit);
          this.map = map;
          advance();
      }

      public final boolean hasNext() { return next != null; }
      public final boolean hasMoreElements() { return next != null; }

      public final void remove() {
          Node<V> p;
        if ((p = lastReturned) == null) {
          throw new IllegalStateException();
        }
          lastReturned = null;
        map.replaceNode(p.key, null, null);
      }
  }


  static final class ValueIterator<V> extends BaseIterator<V>
      implements Iterator<V>, Enumeration<V> {
      ValueIterator(Node<V>[] tab, int size, int index, int limit,
                    ConcurrentLongObjectHashMap<V> map) {
          super(tab, size, index, limit, map);
      }

      public final V next() {
          Node<V> p;
        if ((p = next) == null) {
          throw new NoSuchElementException();
        }
          V v = p.val;
          lastReturned = p;
          advance();
          return v;
      }

      public final V nextElement() { return next(); }
  }

  static final class EntryIterator<V> extends BaseIterator<V>
      implements Iterator<LongEntry<V>> {
      EntryIterator(Node<V>[] tab, int size, int index, int limit,
                    ConcurrentLongObjectHashMap<V> map) {
          super(tab, size, index, limit, map);
      }

      public final LongEntry<V> next() {
          Node<V> p;
        if ((p = next) == null) {
          throw new NoSuchElementException();
        }
          long k = p.key;
          V v = p.val;
          lastReturned = p;
          advance();
        return new SimpleLongEntry<>(k, v);
      }
  }



  static final class ValueSpliterator<V> extends Traverser<V>
      implements Spliterator<V> {
      long est;               // size estimate
      ValueSpliterator(Node<V>[] tab, int size, int index, int limit,
                       long est) {
          super(tab, size, index, limit);
          this.est = est;
      }

      public ValueSpliterator<V> trySplit() {
          int i, f, h;
          return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
              new ValueSpliterator<V>(tab, baseSize, baseLimit = h,
                                        f, est >>>= 1);
      }

      public void forEachRemaining(Consumer<? super V> action) {
          if (action == null) throw new NullPointerException();
        for (Node<V> p; (p = advance()) != null; ) {
          action.accept(p.val);
        }
      }

      public boolean tryAdvance(Consumer<? super V> action) {
          if (action == null) throw new NullPointerException();
          Node<V> p;
        if ((p = advance()) == null) {
          return false;
        }
          action.accept(p.val);
          return true;
      }

      public long estimateSize() { return est; }

      public int characteristics() {
          return Spliterator.CONCURRENT | Spliterator.NONNULL;
      }
  }
  static final class EntrySpliterator<V> extends Traverser<V>
      implements Spliterator<LongEntry<V>> {
      final ConcurrentLongObjectHashMap<V> map; // To export MapEntry
      long est;               // size estimate
      EntrySpliterator(Node<V>[] tab, int size, int index, int limit,
                       long est, ConcurrentLongObjectHashMap<V> map) {
          super(tab, size, index, limit);
          this.map = map;
          this.est = est;
      }

      public EntrySpliterator<V> trySplit() {
          int i, f, h;
          return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
              new EntrySpliterator<V>(tab, baseSize, baseLimit = h,
                                                          f, est >>>= 1, map);
      }

      public void forEachRemaining(Consumer<? super LongEntry<V>> action) {
          if (action == null) throw new NullPointerException();
        for (Node<V> p; (p = advance()) != null; ) {
          action.accept(new SimpleLongEntry<V>(p.key, p.val));
        }
      }

      public boolean tryAdvance(Consumer<? super LongEntry<V>> action) {
          if (action == null) throw new NullPointerException();
          Node<V> p;
        if ((p = advance()) == null) {
          return false;
        }
          action.accept(new SimpleLongEntry<V>(p.key, p.val));
          return true;
      }

      public long estimateSize() { return est; }

      public int characteristics() {
          return Spliterator.DISTINCT | Spliterator.CONCURRENT |
              Spliterator.NONNULL;
      }
  }


  // Parallel bulk operations

    /* ----------------Views -------------- */

  /**
   * Base class for views.
   */
  abstract static class CollectionView<V,E>
      implements Collection<E> {
      final ConcurrentLongObjectHashMap<V> map;
      CollectionView(ConcurrentLongObjectHashMap<V> map)  { this.map = map; }

      /**
       * Returns the map backing this view.
       *
       * @return the map backing this view
       */
      public ConcurrentLongObjectHashMap<V> getMap() { return map; }

      /**
       * Removes all of the elements from this view, by removing all
       * the mappings from the map backing this view.
       */
      public final void clear()      { map.clear(); }
      public final int size()        { return map.size(); }
      public final boolean isEmpty() { return map.isEmpty(); }

      // implementations below rely on concrete classes supplying these
      // abstract methods
      /**
       * Returns an iterator over the elements in this collection.
       *
       * <p>The returned iterator is
       * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
       *
       * @return an iterator over the elements in this collection
       */
      public abstract Iterator<E> iterator();
      public abstract boolean contains(Object o);
      public abstract boolean remove(Object o);

      private static final String OOME_MSG = "Required array size too large";

      public final Object[] toArray() {
          long sz = map.mappingCount();
        if (sz > MAX_ARRAY_SIZE) {
          throw new OutOfMemoryError(OOME_MSG);
        }
          int n = (int)sz;
          Object[] r = new Object[n];
          int i = 0;
          for (E e : this) {
              if (i == n) {
                if (n >= MAX_ARRAY_SIZE) {
                  throw new OutOfMemoryError(OOME_MSG);
                }
                if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1) {
                  n = MAX_ARRAY_SIZE;
                }
                else {
                  n += (n >>> 1) + 1;
                }
                  r = Arrays.copyOf(r, n);
              }
              r[i++] = e;
          }
          return (i == n) ? r : Arrays.copyOf(r, i);
      }

      @SuppressWarnings("unchecked")
      public final <T> T[] toArray(T[] a) {
          long sz = map.mappingCount();
        if (sz > MAX_ARRAY_SIZE) {
          throw new OutOfMemoryError(OOME_MSG);
        }
          int m = (int)sz;
          T[] r = (a.length >= m) ? a :
              (T[])java.lang.reflect.Array
              .newInstance(a.getClass().getComponentType(), m);
          int n = r.length;
          int i = 0;
          for (E e : this) {
              if (i == n) {
                if (n >= MAX_ARRAY_SIZE) {
                  throw new OutOfMemoryError(OOME_MSG);
                }
                if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1) {
                  n = MAX_ARRAY_SIZE;
                }
                else {
                  n += (n >>> 1) + 1;
                }
                  r = Arrays.copyOf(r, n);
              }
              r[i++] = (T)e;
          }
          if (a == r && i < n) {
              r[i] = null; // null-terminate
              return r;
          }
          return (i == n) ? r : Arrays.copyOf(r, i);
      }

      /**
       * Returns a string representation of this collection.
       * The string representation consists of the string representations
       * of the collection's elements in the order they are returned by
       * its iterator, enclosed in square brackets ({@code "[]"}).
       * Adjacent elements are separated by the characters {@code ", "}
       * (comma and space).  Elements are converted to strings as by
       * {@link String#valueOf(Object)}.
       *
       * @return a string representation of this collection
       */
      public final String toString() {
          StringBuilder sb = new StringBuilder();
          sb.append('[');
          Iterator<E> it = iterator();
          if (it.hasNext()) {
              for (;;) {
                  Object e = it.next();
                  sb.append(e == this ? "(this Collection)" : e);
                if (!it.hasNext()) {
                  break;
                }
                  sb.append(',').append(' ');
              }
          }
          return sb.append(']').toString();
      }

      public final boolean containsAll(Collection<?> c) {
          if (c != this) {
              for (Object e : c) {
                if (e == null || !contains(e)) {
                  return false;
                }
              }
          }
          return true;
      }

      public boolean removeAll(Collection<?> c) {
          if (c == null) throw new NullPointerException();
          boolean modified = false;
          // Use (c instanceof Set) as a hint that lookup in c is as
          // efficient as this view
          Node<V>[] t;
          if ((t = map.table) == null) {
              return false;
          } else if (c instanceof Set<?> && c.size() > t.length) {
              for (Iterator<?> it = iterator(); it.hasNext(); ) {
                  if (c.contains(it.next())) {
                      it.remove();
                      modified = true;
                  }
              }
          } else {
            for (Object e : c) {
              modified |= remove(e);
            }
          }
          return modified;
      }

      public final boolean retainAll(Collection<?> c) {
          if (c == null) throw new NullPointerException();
          boolean modified = false;
          for (Iterator<E> it = iterator(); it.hasNext();) {
              if (!c.contains(it.next())) {
                  it.remove();
                  modified = true;
              }
          }
          return modified;
      }

  }

  /**
   * A view of a ConcurrentHashMap as a {@link Collection} of
   * values, in which additions are disabled. This class cannot be
   * directly instantiated. See {@link #values()}.
   */
  static final class ValuesView<V> extends CollectionView<V,V>
      implements Collection<V> {
      ValuesView(ConcurrentLongObjectHashMap<V> map) { super(map); }
      public final boolean contains(Object o) {
          return map.containsValue(o);
      }

      public final boolean remove(Object o) {
          if (o != null) {
              for (Iterator<V> it = iterator(); it.hasNext();) {
                  if (o.equals(it.next())) {
                      it.remove();
                      return true;
                  }
              }
          }
          return false;
      }

      public final Iterator<V> iterator() {
        ConcurrentLongObjectHashMap<V> m = map;
          Node<V>[] t;
          int f = (t = m.table) == null ? 0 : t.length;
          return new ValueIterator<V>(t, f, 0, f, m);
      }

      public final boolean add(V e) {
          throw new UnsupportedOperationException();
      }
      public final boolean addAll(Collection<? extends V> c) {
          throw new UnsupportedOperationException();
      }

      @Override public boolean removeAll(Collection<?> c) {
          if (c == null) throw new NullPointerException();
          boolean modified = false;
          for (Iterator<V> it = iterator(); it.hasNext();) {
              if (c.contains(it.next())) {
                  it.remove();
                  modified = true;
              }
          }
          return modified;
      }

      public boolean removeIf(Predicate<? super V> filter) {
          return map.removeValueIf(filter);
      }

      public Spliterator<V> spliterator() {
          Node<V>[] t;
        ConcurrentLongObjectHashMap<V> m = map;
          long n = m.sumCount();
          int f = (t = m.table) == null ? 0 : t.length;
          return new ValueSpliterator<V>(t, f, 0, f, n < 0L ? 0L : n);
      }

      public void forEach(Consumer<? super V> action) {
          if (action == null) throw new NullPointerException();
          Node<V>[] t;
          if ((t = map.table) != null) {
              Traverser<V> it = new Traverser<V>(t, t.length, 0, t.length);
            for (Node<V> p; (p = it.advance()) != null; ) {
              action.accept(p.val);
            }
          }
      }
  }

  @NotNull
  @Override
  public Iterable<LongEntry<V>> entries() {
    return new EntrySetView<>(this);
  }

  /**
   * A view of a ConcurrentHashMap as a {@link Set} of (key, value)
   * entries.  This class cannot be directly instantiated. See
   * {@link #entrySet()}.
   */
  static final class EntrySetView<V> extends CollectionView<V,LongEntry<V>>
      implements Set<LongEntry<V>> {
      EntrySetView(ConcurrentLongObjectHashMap<V> map) { super(map); }

      public boolean contains(Object o) {
        if (!(o instanceof LongEntry<?>)) return false;
        LongEntry<?> e = (LongEntry<?>)o;
        Object r = map.get(e.getKey());
        if (r == null) return false;
        Object v = e.getValue();
        return v == r || v.equals(r);
      }

    @Override
      public boolean remove(Object o) {
        if (!(o instanceof LongEntry<?>)) return false;
        LongEntry<?> e = (LongEntry<?>)o;
        Object v = e.getValue();
        return map.remove(e.getKey(), v);
      }

      /**
       * @return an iterator over the entries of the backing map
       */
    @NotNull
    @Override
    public Iterator<LongEntry<V>> iterator() {
      ConcurrentLongObjectHashMap<V> m = map;
          Node<V>[] t;
          int f = (t = m.table) == null ? 0 : t.length;
      return new EntryIterator<>(t, f, 0, f, m);
      }

    @Override
    public boolean add(LongEntry<V> e) {
          return map.putVal(e.getKey(), e.getValue(), false) == null;
      }

    @Override
    public boolean addAll(Collection<? extends LongEntry<V>> c) {
          boolean added = false;
      for (LongEntry<V> e : c) {
        if (add(e)) {
                  added = true;
          }
      }
          return added;
      }

    @Override
    public int hashCode() {
          int h = 0;
          Node<V>[] t;
          if ((t = map.table) != null) {
        Traverser<V> it = new Traverser<>(t, t.length, 0, t.length);
              for (Node<V> p; (p = it.advance()) != null; ) {
                  h += p.hashCode();
              }
          }
          return h;
      }

      public final boolean equals(Object o) {
          Set<?> c;
          return ((o instanceof Set) &&
                  ((c = (Set<?>)o) == this ||
                   (containsAll(c) && c.containsAll(this))));
      }

      public Spliterator<LongEntry<V>> spliterator() {
          Node<V>[] t;
        ConcurrentLongObjectHashMap<V> m = map;
          long n = m.sumCount();
          int f = (t = m.table) == null ? 0 : t.length;
          return new EntrySpliterator<V>(t, f, 0, f, n < 0L ? 0L : n, m);
      }

      public void forEach(Consumer<? super LongEntry<V>> action) {
          if (action == null) throw new NullPointerException();
          Node<V>[] t;
          if ((t = map.table) != null) {
              Traverser<V> it = new Traverser<V>(t, t.length, 0, t.length);
            for (Node<V> p; (p = it.advance()) != null; ) {
              action.accept(new SimpleLongEntry<V>(p.key, p.val));
            }
          }
      }

  }

  private static class SimpleLongEntry<V> implements LongEntry<V> {
    private final long key;
    private final V value;

    private SimpleLongEntry(long key, @NotNull V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public long getKey() {
      return key;
    }

    @Override
    public @NotNull V getValue() {
      return value;
    }
  }

  // -------------------------------------------------------


  // Unsafe mechanics
  private static final long SIZECTL;
  private static final long TRANSFERINDEX;
  private static final long BASECOUNT;
  private static final long CELLSBUSY;
  private static final long CELLVALUE;
  private static final long ABASE;
  private static final int ASHIFT;

  static {
    try {
      Class<?> k = ConcurrentLongObjectHashMap.class;
      SIZECTL = Unsafe.objectFieldOffset(k.getDeclaredField("sizeCtl"));
      TRANSFERINDEX = Unsafe.objectFieldOffset(k.getDeclaredField("transferIndex"));
      BASECOUNT = Unsafe.objectFieldOffset(k.getDeclaredField("baseCount"));
      CELLSBUSY = Unsafe.objectFieldOffset(k.getDeclaredField("cellsBusy"));
      CELLVALUE = Unsafe.objectFieldOffset(CounterCell.class.getDeclaredField("value"));
      ABASE = Unsafe.arrayBaseOffset(Node[].class);
      int scale = Unsafe.arrayIndexScale(Node[].class);
      if ((scale & (scale - 1)) != 0) {
        throw new Error("data type scale not a power of two");
      }
      ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
    }
    catch (Throwable t) {
      throw new Error(t);
    }


      // Reduce the risk of rare disastrous classloading in first call to
      // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
      Class<?> ensureLoaded = LockSupport.class;

      // Eager class load observed to help JIT during startup
      ensureLoaded = ReservationNode.class;
  }

  /**
   * @return value if there is no entry in the map, or corresponding value if entry already exists
   */
  @Override
  @NotNull
  public V cacheOrGet(final long key, @NotNull final V defaultValue) {
    V v = get(key);
    if (v != null) return v;
    V prev = putIfAbsent(key, defaultValue);
    return prev == null ? defaultValue : prev;
  }
}
