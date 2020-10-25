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
class UseOfTypeVariableAsTypeArgument {
  interface Lib<T extends @Nullable Object> {}

  interface Super<T extends @Nullable Object> {
    Lib<T> get();
  }

  interface SubObject extends Super<Object> {}

  interface SubObjectUnspec extends Super<@NullnessUnspecified Object> {}

  interface SubObjectUnionNull extends Super<@Nullable Object> {}

  class Caller {
    Lib<? extends Object> x0(SubObject s) {
      return s.get();
    }

    Lib<? extends Object> x1(SubObjectUnspec s) {
      // jspecify_nullness_not_enough_information
      return s.get();
    }

    Lib<? extends Object> x2(SubObjectUnionNull s) {
      // jspecify_nullness_mismatch
      return s.get();
    }

    Lib<? extends Object> x3(Super<Object> s) {
      return s.get();
    }

    Lib<? extends Object> x4(Super<@NullnessUnspecified Object> s) {
      // jspecify_nullness_not_enough_information
      return s.get();
    }

    Lib<? extends Object> x5(Super<@Nullable Object> s) {
      // jspecify_nullness_mismatch
      return s.get();
    }

    Lib<? extends Object> x6(Super<? extends Object> s) {
      return s.get();
    }

    Lib<? extends Object> x7(Super<? extends @NullnessUnspecified Object> s) {
      // jspecify_nullness_not_enough_information
      return s.get();
    }

    Lib<? extends Object> x8(Super<? extends @Nullable Object> s) {
      // jspecify_nullness_mismatch
      return s.get();
    }

    Lib<? extends Object> x9(Super<?> s) {
      // jspecify_nullness_mismatch
      return s.get();
    }
  }
}
