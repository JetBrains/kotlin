import java.util.*;

interface wildcardExtendsObject {
    interface SuperWildcardExtendsObject<T> {
        List<? extends T> typeForSubstitute();
    }

    interface MidWildcardExtendsObject extends SuperWildcardExtendsObject<Object> {
    }
}