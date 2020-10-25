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
class MultiBoundTypeVariableUnionNullToOtherUnionNull {
  <T extends Object & Lib> @Nullable Lib x0(@Nullable T x) {
    return x;
  }

  <T extends Object & @NullnessUnspecified Lib> @Nullable Lib x1(@Nullable T x) {
    return x;
  }

  <T extends Object & @Nullable Lib> @Nullable Lib x2(@Nullable T x) {
    return x;
  }

  <T extends @NullnessUnspecified Object & Lib> @Nullable Lib x3(@Nullable T x) {
    return x;
  }

  <T extends @NullnessUnspecified Object & @NullnessUnspecified Lib> @Nullable Lib x4(
      @Nullable T x) {
    return x;
  }

  <T extends @NullnessUnspecified Object & @Nullable Lib> @Nullable Lib x5(@Nullable T x) {
    return x;
  }

  <T extends @Nullable Object & Lib> @Nullable Lib x6(@Nullable T x) {
    return x;
  }

  <T extends @Nullable Object & @NullnessUnspecified Lib> @Nullable Lib x7(@Nullable T x) {
    return x;
  }

  <T extends @Nullable Object & @Nullable Lib> @Nullable Lib x8(@Nullable T x) {
    return x;
  }

  interface Lib {}
}
