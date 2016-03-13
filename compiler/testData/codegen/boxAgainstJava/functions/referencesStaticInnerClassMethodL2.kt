// FILE: R.java

class R {
    public static class id {
        public static class zzz {
            public static final int main = 17;
        }
    }
}

// FILE: 1.kt

fun box() =
    if (R.id.zzz.main == 17) "OK" else "fail"
