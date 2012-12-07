package test;

import java.util.List;
import jet.runtime.typeinfo.KotlinSignature;

public interface PropagateTypeArgumentNullable {

    public interface Super {

        @KotlinSignature("fun outS(p : List<String?>)")
        void outS(List<String> p);

        @KotlinSignature("fun invOutS(p : MutableList<List<String?>>)")
        void invOutS(List<List<String>> p);

        @KotlinSignature("fun outOutS(p : List<List<String?>>)")
        void outOutS(List<List<String>> p);

        @KotlinSignature("fun outR() : List<String?>")
        List<String> outR();

        @KotlinSignature("fun invR() : MutableList<String?>")
        List<String> invR();

        @KotlinSignature("fun invOutR() : MutableList<List<String?>>")
        List<List<String>> invOutR();
    }

    public interface Sub extends Super {
        void outS(List<String> p);

        void invOutS(List<List<String>> p);

        void outOutS(List<List<String>> p);

        List<String> outR();

        List<String> invR();

        List<List<String>> invOutR();
    }
}
