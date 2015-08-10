public class J {
    private final int param;

    public J(int param) {
        this.param = param;
    }

    public String foo(int[] arr, Object[] arr2, Integer y) {
        return "" + param + arr[0] + arr2[0] + y;
    }
}
