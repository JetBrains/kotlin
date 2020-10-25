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

class NotNullAwareAnnotatedInnerOfNonParameterized {
  interface Lib<T extends @Nullable Object> {}

  class Nested {
    class DoublyNested {}
  }

  @Nullable Nested x4;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable NotNullAwareAnnotatedInnerOfNonParameterized.Nested x5;

  NotNullAwareAnnotatedInnerOfNonParameterized.@Nullable Nested x6;

  // jspecify_nullness_intrinsically_not_nullable
  @Nullable NotNullAwareAnnotatedInnerOfNonParameterized.Nested.DoublyNested x7;

  // jspecify_nullness_intrinsically_not_nullable
  NotNullAwareAnnotatedInnerOfNonParameterized.@Nullable Nested.DoublyNested x8;

  NotNullAwareAnnotatedInnerOfNonParameterized.Nested.@Nullable DoublyNested x9;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<@Nullable NotNullAwareAnnotatedInnerOfNonParameterized.Nested.DoublyNested> l1;

  // jspecify_nullness_intrinsically_not_nullable
  Lib<NotNullAwareAnnotatedInnerOfNonParameterized.@Nullable Nested.DoublyNested> l2;

  Lib<NotNullAwareAnnotatedInnerOfNonParameterized.Nested.DoublyNested> l3;
}
