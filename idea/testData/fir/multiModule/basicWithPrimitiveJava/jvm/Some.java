public class Some {
    public boolean foo(int param) {
        return param > 0;
    }

    public String[] bar(int[] arr) {
        String[] result = new String[arr.length];
        int i = 0;
        for (int elem: arr) {
            result[i++] = elem;
        }
        return result;
    }
}