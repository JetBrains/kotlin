public class J {
    abstract static public class AImpl {
        public char charAt(int index) {
            return 'A';
        }

        public final int length() { return 56; }
    }

    public static class A extends AImpl implements CharSequence {
        public CharSequence subSequence(int start, int end) {
            return null;
        }
    }
}
