package testing;

import testing.rename.RenamePackage;

class JavaClientOldFacade {
    public void foo() {
        String old = RenamePackage.getBar();
        RenamePackage.setBar(old + "new");
    }
}