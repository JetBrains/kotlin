package testing;

import testing.rename.*;

class GroovyClient {
    public void foo(AP ap, DP dp) {
        ap.second
        new BP().second
        new CP().second

        dp.second
        new EP().second
        new FP().second
    }

    public interface DP extends AP {
    }

    public static class EP implements DP {
        @Override
        public int getSecond() {
            return 3;
        }
    }

    public static class FP extends EP {
        @Override
        public int getSecond() {
            return 4;
        }
    }
}
