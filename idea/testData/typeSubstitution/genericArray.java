interface genericArray {
    interface SuperGenericArray<T> {
        T typeForSubstitute();
    }

    interface MidGenericArray<T> extends SuperGenericArray<T[]> {
    }
}