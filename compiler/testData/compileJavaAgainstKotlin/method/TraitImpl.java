package test;

import java.util.ArrayList;

abstract class TraitImpl implements Trait {
    {
        Trait.DefaultImpls.simple(this);

        Trait.DefaultImpls.generic(this, new ArrayList<String>());
    }
}
