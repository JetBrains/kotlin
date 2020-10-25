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
class SameTypeTypeVariable<T extends @Nullable Object> {
  Lib<T> x0(Lib<T> x) {
    return x;
  }

  Lib<T> x1(Lib<@NullnessUnspecified T> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<T> x2(Lib<@Nullable T> x) {
    // jspecify_nullness_mismatch
    return x;
  }

  Lib<@NullnessUnspecified T> x3(Lib<T> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@NullnessUnspecified T> x4(Lib<@NullnessUnspecified T> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@NullnessUnspecified T> x5(Lib<@Nullable T> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@Nullable T> x6(Lib<T> x) {
    // jspecify_nullness_mismatch
    return x;
  }

  Lib<@Nullable T> x7(Lib<@NullnessUnspecified T> x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  Lib<@Nullable T> x8(Lib<@Nullable T> x) {
    return x;
  }

  interface Lib<T extends @Nullable Object> {}
}
