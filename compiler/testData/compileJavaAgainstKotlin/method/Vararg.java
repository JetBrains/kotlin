package test;

import java.util.List;
import java.util.ArrayList;

class Vararg {
    {
        List<String> list = new ArrayList<String>();
        List<String> r = VarargKt.gg(list, 3, 4, 5, 6);
    }
}
