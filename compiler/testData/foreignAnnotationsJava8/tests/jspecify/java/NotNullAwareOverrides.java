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

import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullnessUnspecified;

class NotNullAwareOverrides {
  interface Super {
    Object makeObject();

    @NullnessUnspecified
    Object makeObjectUnspec();

    @Nullable
    Object makeObjectUnionNull();
  }

  interface SubObject extends Super {
    @Override
    // jspecify_nullness_not_enough_information
    Object makeObject();

    @Override
    // jspecify_nullness_not_enough_information
    Object makeObjectUnspec();

    @Override
    Object makeObjectUnionNull();
  }

  interface SubObjectUnspec extends Super {
    @Override
    @NullnessUnspecified
    // jspecify_nullness_not_enough_information
    Object makeObject();

    @Override
    @NullnessUnspecified
    // jspecify_nullness_not_enough_information
    Object makeObjectUnspec();

    @Override
    @NullnessUnspecified
    Object makeObjectUnionNull();
  }

  interface SubObjectUnionNull extends Super {
    @Override
    @Nullable
    // jspecify_nullness_not_enough_information
    Object makeObject();

    @Override
    @Nullable
    // jspecify_nullness_not_enough_information
    Object makeObjectUnspec();

    @Override
    @Nullable
    Object makeObjectUnionNull();
  }
}
