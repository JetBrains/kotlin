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
class TypeVariableToSelf<
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
  Never1T x0(Never1T x) {
    return x;
  }

  ChildOfNever1T x1(ChildOfNever1T x) {
    return x;
  }

  UnspecChildOfNever1T x2(UnspecChildOfNever1T x) {
    return x;
  }

  NullChildOfNever1T x3(NullChildOfNever1T x) {
    return x;
  }

  Never2T x4(Never2T x) {
    return x;
  }

  ChildOfNever2T x5(ChildOfNever2T x) {
    return x;
  }

  UnspecChildOfNever2T x6(UnspecChildOfNever2T x) {
    return x;
  }

  NullChildOfNever2T x7(NullChildOfNever2T x) {
    return x;
  }

  UnspecT x8(UnspecT x) {
    return x;
  }

  ChildOfUnspecT x9(ChildOfUnspecT x) {
    return x;
  }

  UnspecChildOfUnspecT x10(UnspecChildOfUnspecT x) {
    return x;
  }

  NullChildOfUnspecT x11(NullChildOfUnspecT x) {
    return x;
  }

  ParametricT x12(ParametricT x) {
    return x;
  }

  ChildOfParametricT x13(ChildOfParametricT x) {
    return x;
  }

  UnspecChildOfParametricT x14(UnspecChildOfParametricT x) {
    return x;
  }

  NullChildOfParametricT x15(NullChildOfParametricT x) {
    return x;
  }
}
