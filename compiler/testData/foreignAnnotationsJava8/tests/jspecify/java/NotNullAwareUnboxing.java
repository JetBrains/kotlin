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

class NotNullAwareUnboxing {
  @DefaultNonNull
  interface Super {
    Integer getInteger();

    @NullnessUnspecified
    Integer getIntegerUnspec();

    @Nullable
    Integer getIntegerUnionNull();
  }

  abstract class Sub implements Super {
    int x0() {
      return getInteger();
    }

    int x1() {
      // jspecify_nullness_not_enough_information
      return getIntegerUnspec();
    }

    int x2() {
      // jspecify_nullness_mismatch
      return getIntegerUnionNull();
    }

    long x3() {
      return getInteger();
    }

    long x4() {
      // jspecify_nullness_not_enough_information
      return getIntegerUnspec();
    }

    long x5() {
      // jspecify_nullness_mismatch
      return getIntegerUnionNull();
    }
  }
}
