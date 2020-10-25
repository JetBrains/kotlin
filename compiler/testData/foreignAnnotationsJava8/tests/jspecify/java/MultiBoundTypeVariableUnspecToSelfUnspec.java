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
class MultiBoundTypeVariableUnspecToSelfUnspec {
  <T extends Object & Lib> @NullnessUnspecified T x0(@NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends Object & @NullnessUnspecified Lib> @NullnessUnspecified T x1(
      @NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends Object & @Nullable Lib> @NullnessUnspecified T x2(@NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @NullnessUnspecified Object & Lib> @NullnessUnspecified T x3(
      @NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @NullnessUnspecified Object & @NullnessUnspecified Lib> @NullnessUnspecified T x4(
      @NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @NullnessUnspecified Object & @Nullable Lib> @NullnessUnspecified T x5(
      @NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @Nullable Object & Lib> @NullnessUnspecified T x6(@NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @Nullable Object & @NullnessUnspecified Lib> @NullnessUnspecified T x7(
      @NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  <T extends @Nullable Object & @Nullable Lib> @NullnessUnspecified T x8(@NullnessUnspecified T x) {
    // jspecify_nullness_not_enough_information
    return x;
  }

  interface Lib {}
}
