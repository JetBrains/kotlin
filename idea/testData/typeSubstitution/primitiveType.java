interface primitiveType {
    interface SuperPrimitive<T> {
        int typeForSubstitute();
    }

    interface MidPrimitive extends SuperPrimitive<Integer> {
    }
}