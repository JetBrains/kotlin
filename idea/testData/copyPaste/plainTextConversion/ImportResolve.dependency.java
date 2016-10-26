package test;

public class ToBeImportedJava {
    public static final String TO_BE_IMPORTED_CONST = "!!!";

    public static void staticMethod() {
    }
}

public interface IAmbiguousJava {

}

public interface AmbiguousJava {

}

public interface IAmbiguous {

}

public interface Ambiguous {

}

public class Z {
    public interface IAmbiguousJava {

    }

    public interface AmbiguousJava {

    }
}
