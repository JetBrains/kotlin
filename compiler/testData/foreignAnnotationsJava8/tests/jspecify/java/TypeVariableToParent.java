/*
 * Copyright 2020 The JSpecify Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jspecify.annotations.DefaultNonNull;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullnessUnspecified;

@DefaultNonNull
class TypeVariableToParent<
    Never1T,
    ChildOfNever1T extends Never1T,
    UnspecChildOfNever1T extends @NullnessUnspecified Never1T,
    NullChildOfNever1T extends @Nullable Never1T,
    //
    Never2T extends Object,
    ChildOfNever2T extends Never2T,
    UnspecChildOfNever2T extends @NullnessUnspecified Never2T,
    NullChildOfNever2T extends @Nullable Never2T,
    //
    UnspecT extends @NullnessUnspecified Object,
    ChildOfUnspecT extends UnspecT,
    UnspecChildOfUnspecT extends @NullnessUnspecified UnspecT,
    NullChildOfUnspecT extends @Nullable UnspecT,
    //
    ParametricT extends @Nullable Object,
    ChildOfParametricT extends ParametricT,
    UnspecChildOfParametricT extends @NullnessUnspecified ParametricT,
    NullChildOfParametricT extends @Nullable ParametricT,
    //
    UnusedT> {
  Never1T x0(ChildOfNever1T x) {
    return x;
  }

  Never1T x1(UnspecChildOfNever1T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Never1T x2(NullChildOfNever1T x) {
    // jspecify_nullness_mismatch
    return x;
  }

  Never2T x3(ChildOfNever2T x) {
    return x;
  }

  Never2T x4(UnspecChildOfNever2T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Never2T x5(NullChildOfNever2T x) {
    // jspecify_nullness_mismatch
    return x;
  }

  UnspecT x6(ChildOfUnspecT x) {
    return x;
  }

  UnspecT x7(UnspecChildOfUnspecT x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  UnspecT x8(NullChildOfUnspecT x) {
    // jspecify_nullness_mismatch
    return x;
  }

  ParametricT x9(ChildOfParametricT x) {
    return x;
  }

  ParametricT x10(UnspecChildOfParametricT x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  ParametricT x11(NullChildOfParametricT x) {
    // jspecify_nullness_mismatch
    return x;
  }
}
