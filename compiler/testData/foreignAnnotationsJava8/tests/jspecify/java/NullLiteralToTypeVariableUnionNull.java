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
class NullLiteralToTypeVariableUnionNull<
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
  @Nullable
  Never1T x0() {
    return null;
  }

  @Nullable
  ChildOfNever1T x1() {
    return null;
  }

  @Nullable
  UnspecChildOfNever1T x2() {
    return null;
  }

  @Nullable
  NullChildOfNever1T x3() {
    return null;
  }

  @Nullable
  Never2T x4() {
    return null;
  }

  @Nullable
  ChildOfNever2T x5() {
    return null;
  }

  @Nullable
  UnspecChildOfNever2T x6() {
    return null;
  }

  @Nullable
  NullChildOfNever2T x7() {
    return null;
  }

  @Nullable
  UnspecT x8() {
    return null;
  }

  @Nullable
  ChildOfUnspecT x9() {
    return null;
  }

  @Nullable
  UnspecChildOfUnspecT x10() {
    return null;
  }

  @Nullable
  NullChildOfUnspecT x11() {
    return null;
  }

  @Nullable
  ParametricT x12() {
    return null;
  }

  @Nullable
  ChildOfParametricT x13() {
    return null;
  }

  @Nullable
  UnspecChildOfParametricT x14() {
    return null;
  }

  @Nullable
  NullChildOfParametricT x15() {
    return null;
  }
}
