import java.util.*;

interface selfReference {
    interface SuperSelfRef<T extends SuperSelfRef<T>> {
        public List<T> typeForSubstitute();
    }

    interface MidSelfRef extends SuperSelfRef<MidSelfRef> {
    }
}