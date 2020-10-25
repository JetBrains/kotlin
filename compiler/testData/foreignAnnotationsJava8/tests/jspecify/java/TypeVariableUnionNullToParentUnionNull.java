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
class TypeVariableUnionNullToParentUnionNull<
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
  Never1T x0(@Nullable ChildOfNever1T x) {
    return x;
  }

  @Nullable
  Never1T x1(@Nullable UnspecChildOfNever1T x) {
    return x;
  }

  @Nullable
  Never1T x2(@Nullable NullChildOfNever1T x) {
    return x;
  }

  @Nullable
  Never2T x3(@Nullable ChildOfNever2T x) {
    return x;
  }

  @Nullable
  Never2T x4(@Nullable UnspecChildOfNever2T x) {
    return x;
  }

  @Nullable
  Never2T x5(@Nullable NullChildOfNever2T x) {
    return x;
  }

  @Nullable
  UnspecT x6(@Nullable ChildOfUnspecT x) {
    return x;
  }

  @Nullable
  UnspecT x7(@Nullable UnspecChildOfUnspecT x) {
    return x;
  }

  @Nullable
  UnspecT x8(@Nullable NullChildOfUnspecT x) {
    return x;
  }

  @Nullable
  ParametricT x9(@Nullable ChildOfParametricT x) {
    return x;
  }

  @Nullable
  ParametricT x10(@Nullable UnspecChildOfParametricT x) {
    return x;
  }

  @Nullable
  ParametricT x11(@Nullable NullChildOfParametricT x) {
    return x;
  }
}
