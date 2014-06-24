import java.util.*;

interface rawTypeInDeclaration {
    interface Super<T> {
        List typeForSubstitute();
    }

    interface Sub<U> extends Super<U> {
    }
}