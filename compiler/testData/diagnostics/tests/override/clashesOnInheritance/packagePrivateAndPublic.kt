// FIR_IDENTICAL
// FILE: ViewModel.java
package viewmodel;

public class ViewModel {
    final void clear() {
    }
}

// FILE: samePackage.kt
package viewmodel

interface IMyViewModel {
    fun clear()
}

class MyViewModel: ViewModel(), IMyViewModel {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun clear() = Unit
}

// FILE: differentPackage.kt
package different

import viewmodel.IMyViewModel
import viewmodel.ViewModel

class MyViewModel: ViewModel(), IMyViewModel {
    override fun clear() = Unit
}
