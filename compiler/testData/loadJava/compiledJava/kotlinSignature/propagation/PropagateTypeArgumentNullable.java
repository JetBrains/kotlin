package test;

import java.util.List;

public interface PropagateTypeArgumentNullable {

    public interface Super {
        void outS(List<String> p);

        void invOutS(List<List<String>> p);

        void outOutS(List<List<String>> p);

        List<String> outR();

        List<String> invR();

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
