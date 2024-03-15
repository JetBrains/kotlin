package org.jetbrains.java.decompiler.modules.decompiler.sforms;

import org.jetbrains.java.decompiler.modules.decompiler.ValidationHelper;
import org.jetbrains.java.decompiler.util.SFormsFastMapDirect;

/*
 * The VarMapHolder class is used to hold the variable maps and
 * is used to handle logic related to section 16 of the java specification.
 *
 * It can be in 2 different states:
 * - normal when `ifFalse` is null
 * - split when `ifFalse` is not null
 *
 * In the split state, the `ifTrue` field represents the variable data
 * when the expression is true, and the `ifFalse` field represents the
 * variable data when the expression is false.
 *
 * If the variable data is the same for `ifTrue` and `ifFalse`, then
 * the holder will be in a normal state instead of a split state.
 * Which means that if the holder is in a split state, that the
 * `ifTrue` and `ifFalse` fields are not the same object and therefore
 * they can be safely modified.
 */
final class VarMapHolder {
  private SFormsFastMapDirect ifTrue;  // not null
  private SFormsFastMapDirect ifFalse; // nullable

  private VarMapHolder(SFormsFastMapDirect ifTrue, SFormsFastMapDirect ifFalse) {
    this.ifTrue = ifTrue;
    this.ifFalse = ifFalse;
  }

  static VarMapHolder ofNormal(SFormsFastMapDirect holder) {
    return new VarMapHolder(new SFormsFastMapDirect(holder), null);
  }

  /**
   * Gets the variable map for the related expression, in case it is true.
   * The caller should not mutate this map unless
   * `makeFullyMutable()` has been called or `isNormal()` returned false.
  */
  SFormsFastMapDirect getIfTrue() {
    ValidationHelper.assertTrue(
      this.ifTrue != null && this.ifTrue != this.ifFalse,
      "VarMapHolder is in an illegal state");
    return this.ifTrue;
  }

  /**
   * Gets the variable map for the related expression, in case it is false.
   * The caller should not mutate this map unless
   * `makeFullyMutable()` has been called or `isNormal()` returned false.
   */
  SFormsFastMapDirect getIfFalse() {
    ValidationHelper.assertTrue(
      this.ifTrue != null && this.ifTrue != this.ifFalse,
      "VarMapHolder is in an illegal state");
    return this.ifFalse == null ? this.ifTrue : this.ifFalse;
  }

  /**
   * Merge the holder from a split state to a normal state. If this holder is
   * already in a normal state, this method does nothing.
   */
  SFormsFastMapDirect toNormal() {
    final SFormsFastMapDirect result = mergeMaps(this.ifTrue, this.ifFalse);
    this.ifFalse = null;
    return result;
  }

  /**
   * Gets the variable map for the related expression. Note: this should only be called
   * when the holder is in a normal state.
   */
  SFormsFastMapDirect getNormal() {
    this.assertIsNormal();

    return this.ifTrue;
  }

  void assertIsNormal() {
    ValidationHelper.assertTrue(this.isNormal(), "VarMapHolder is not in normal state");
  }

  /**
   * Sets the "ifTrue" variable map to the given map.
   */
  void setIfTrue(SFormsFastMapDirect ifTrue) {
    if (this.ifTrue != ifTrue && this.ifFalse == null) {
      // make sure we don't override getIfFalse()
      this.ifFalse = this.ifTrue;
    } else if (this.ifFalse == ifTrue) {
      this.ifFalse = null; // go back to normal state
    }

    this.ifTrue = ifTrue;
  }

  /**
   * Sets the "ifFalse" variable map to the given map.
   */
  void setIfFalse(SFormsFastMapDirect ifFalse) {
    if (this.ifTrue == ifFalse) {
      this.ifTrue = null; // go back to normal state
    } else {
      this.ifFalse = ifFalse;
    }
  }

  /**
   * Sets the "normal" variable map to the given map.
   */
  void setNormal(SFormsFastMapDirect normal) {
    this.ifFalse = null;
    this.ifTrue = normal;
  }

  /**
   * Sets this holder's maps to the given maps.
   * This does not create copies. Any mutations to the maps of
   * one holder will be reflected in the other holder.
   */
  public void set(VarMapHolder bVarMaps) {
    this.ifTrue = bVarMaps.ifTrue;
    this.ifFalse = bVarMaps.ifFalse;
  }

  /**
   * Merge the given variable map with the "ifTrue" map.
   */
  void mergeIfTrue(SFormsFastMapDirect map2) {
    if(this.ifTrue == map2 || map2 == null || map2.isEmpty()) {
      return;
    }

    this.makeFullyMutable();
    this.ifTrue.union(map2);
  }

  /**
   * Merge the given variable map with the "ifFalse" map.
   */
  void mergeIfFalse(SFormsFastMapDirect map2) {
    if(this.ifFalse == map2 || map2 == null || map2.isEmpty()) {
      return;
    }

    this.makeFullyMutable();
    this.ifFalse.union(map2);
  }

  /**
   * Merge the given variable map with the "normal" map.
   */
  void mergeNormal(SFormsFastMapDirect map2) {
    this.assertIsNormal();

    if(this.ifTrue == map2 || map2 == null || map2.isEmpty()) {
      return;
    }

    this.ifTrue.union(map2);
  }

  boolean isNormal() {
    return this.ifFalse == null;
  }

  /**
   * Swaps the "ifTrue" and "ifFalse" maps.
   */
  void swap() {
    if(this.ifFalse == null) {
      return;
    }

    final SFormsFastMapDirect tmp = this.ifTrue;
    this.ifTrue = this.ifFalse;
    this.ifFalse = tmp;
  }

  /**
   * Makes sure that the returned values from getIfTrue() and getIfFalse()
   * are 2 different maps, and therefore can be mutated without affecting
   * the other map.
   */
  void makeFullyMutable() {
    if(this.ifFalse != null) {
      ValidationHelper.assertTrue(this.ifTrue != this.ifFalse, "VarMapHolder is in an illegal state");
      return;
    }

    this.ifFalse = new SFormsFastMapDirect(this.ifTrue);
  }


  /**
   * Merge the variable data from 2 maps.
   * @param mapTo The first map to merge. Will be mutated.
   *               The result will be stored in this map.
   * @param map2 The second map to merge. Will not be mutated. Nullable.
   */
  static SFormsFastMapDirect mergeMaps(SFormsFastMapDirect mapTo, SFormsFastMapDirect map2) {

    if (mapTo != map2 && map2 != null && !map2.isEmpty()) {
      mapTo.union(map2);
    }

    return mapTo;
  }

  void removeAllFields() {
    if(this.ifFalse != null) {
      this.ifFalse.removeAllFields();
    }
    this.ifTrue.removeAllFields();
  }
}
