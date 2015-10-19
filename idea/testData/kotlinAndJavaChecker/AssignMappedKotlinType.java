import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import test.*;

class AssignMappedKotlinType {
    void test() {
        int i1 = AssignMappedKotlinTypeKt.getInt();
        Integer i2 = AssignMappedKotlinTypeKt.getInt();
        Number number = AssignMappedKotlinTypeKt.getNumber();
        String str = AssignMappedKotlinTypeKt.getString();

        Collection<Integer> intCollection = AssignMappedKotlinTypeKt.getList();
        List<Integer> intList = AssignMappedKotlinTypeKt.getList();

        Collection<Integer> intMutableCollection = AssignMappedKotlinTypeKt.getMutableList();
        List<Integer> intMutableList = AssignMappedKotlinTypeKt.getMutableList();

        Collection<String> stringsCollection = AssignMappedKotlinTypeKt.getArrayList();
        ArrayList<String> arrayListCollection = AssignMappedKotlinTypeKt.getArrayList();
    }
}
