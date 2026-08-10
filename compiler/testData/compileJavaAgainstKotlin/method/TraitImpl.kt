// FILE: TraitImpl.java
package test;

import java.util.ArrayList;

abstract class TraitImpl implements Trait {
    {
        Trait.DefaultImpls.simple(this);

        Trait.DefaultImpls.generic(this, new ArrayList<String>());
    }
}

// FILE: TraitImpl.kt
package test

interface Trait {
    fun simple() {
    }

    fun generic(list: List<String>) {
    }
}
