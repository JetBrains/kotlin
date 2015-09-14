package testing;

import testing.rename.RenamePackage;

class JavaClientOldFacade {
    public void foo() {
        String old = RenamePackage.getFoo();
        RenamePackage.setFoo(old + "new");
    }
}