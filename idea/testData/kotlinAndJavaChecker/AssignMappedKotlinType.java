import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import test.TestPackage;

class AssignMappedKotlinType {
    void test() {
        int i1 = TestPackage.getInt();
        Integer i2 = TestPackage.getInt();
        Number number = TestPackage.getNumber();
        String str = TestPackage.getString();

        Collection<Integer> intCollection = TestPackage.getList();
        List<Integer> intList = TestPackage.getList();

        Collection<Integer> intMutableCollection = TestPackage.getMutableList();
        List<Integer> intMutableList = TestPackage.getMutableList();

        Collection<String> stringsCollection = TestPackage.getArrayList();
        ArrayList<String> arrayListCollection = TestPackage.getArrayList();
    }
}
