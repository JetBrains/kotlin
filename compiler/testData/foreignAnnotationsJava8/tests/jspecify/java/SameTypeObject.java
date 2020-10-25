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
class SameTypeObject {
  Lib<Object> x0(Lib<Object> x) {
    return x;
  }

  Lib<Object> x1(Lib<@NullnessUnspecified Object> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<Object> x2(Lib<@Nullable Object> x) {
    // jspecify_nullness_mismatch
    return x;
  }

  Lib<@NullnessUnspecified Object> x3(Lib<Object> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@NullnessUnspecified Object> x4(Lib<@NullnessUnspecified Object> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@NullnessUnspecified Object> x5(Lib<@Nullable Object> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@Nullable Object> x6(Lib<Object> x) {
    // jspecify_nullness_mismatch
    return x;
  }

  Lib<@Nullable Object> x7(Lib<@NullnessUnspecified Object> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@Nullable Object> x8(Lib<@Nullable Object> x) {
    return x;
  }

  interface Lib<T extends @Nullable Object> {}
}
