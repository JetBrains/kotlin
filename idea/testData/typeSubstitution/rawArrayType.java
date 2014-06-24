interface rawArrayType {
    interface SuperArray<T> {
        T[] typeForSubstitute();
    }

    interface MidArray extends SuperArray {
    }
}