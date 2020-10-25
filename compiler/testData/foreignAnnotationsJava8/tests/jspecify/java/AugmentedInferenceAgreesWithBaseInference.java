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

@DefaultNonNull
abstract class AugmentedInferenceAgreesWithBaseInference {
  void x(Foo<Object> a, Foo<@Nullable Object> b) {
    // List of possibly heterogeneous Foo types.
    List<Foo<?>> l1 = makeList(a, b);

    /*
     * List of some unspecified homogeneous Foo type. There is such a type under plain Java (since
     * the base type for both is Foo<Object>) but not under JSpecify (since one is Foo<Object> and
     * the other is Foo<@Nullable Object>).
     *
     * Notice that `makeList(a, b)` is fine "in a vacuum" even under JSpecify (as shown above). Only
     * here, where the type of the expression is forced to conform to the target type, is there a
     * problem.
     */
    // jspecify_nullness_mismatch
    List<? extends Foo<?>> l2 = makeList(a, b);
  }

  abstract <T extends @Nullable Object> List<T> makeList(T a, T b);

  interface Foo<T extends @Nullable Object> {}

  interface List<T> {}
}
