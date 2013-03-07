package test;

import java.util.List;

public interface ErrorTypes {
    interface Super {
        void errorTypeInParameter(List<T[]> list);

        T returnErrorType();
    }

    interface Sub extends Super {
        void errorTypeInParameter(List<T[]> list);

        T  returnErrorType();
    }
}
