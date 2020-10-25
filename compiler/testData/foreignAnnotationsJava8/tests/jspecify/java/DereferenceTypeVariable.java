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
class DereferenceTypeVariable<
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
  void x0(Never1T x) {
    synchronized (x) {
    }
  }

  void x1(ChildOfNever1T x) {
    synchronized (x) {
    }
  }

  void x2(UnspecChildOfNever1T x) {
    // jspecify_nullness_not_enough_information
    synchronized (x) {
    }
  }

  void x3(NullChildOfNever1T x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x4(Never2T x) {
    synchronized (x) {
    }
  }

  void x5(ChildOfNever2T x) {
    synchronized (x) {
    }
  }

  void x6(UnspecChildOfNever2T x) {
    // jspecify_nullness_not_enough_information
    synchronized (x) {
    }
  }

  void x7(NullChildOfNever2T x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x8(UnspecT x) {
    // jspecify_nullness_not_enough_information
    synchronized (x) {
    }
  }

  void x9(ChildOfUnspecT x) {
    // jspecify_nullness_not_enough_information
    synchronized (x) {
    }
  }

  void x10(UnspecChildOfUnspecT x) {
    // jspecify_nullness_not_enough_information
    synchronized (x) {
    }
  }

  void x11(NullChildOfUnspecT x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x12(ParametricT x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x13(ChildOfParametricT x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x14(UnspecChildOfParametricT x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }

  void x15(NullChildOfParametricT x) {
    // jspecify_nullness_mismatch
    synchronized (x) {
    }
  }
}
