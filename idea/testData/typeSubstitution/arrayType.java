interface arrayType {
    interface SuperArray<T> {
        T[] typeForSubstitute();
    }

    interface MidArray extends SuperArray<Integer> {
    }
}