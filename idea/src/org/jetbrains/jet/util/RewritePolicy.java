package org.jetbrains.jet.util;

/**
 * @author abreslav
 */
public interface RewritePolicy {

    RewritePolicy DO_NOTHING = new RewritePolicy() {
        @Override
        public <K> boolean rewriteProcessingNeeded(K key) {
            return false;
        }

        @Override
        public <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }
    };

    <K> boolean rewriteProcessingNeeded(K key);

    // True to put, false to skip
    <K, V> boolean processRewrite(WritableSlice<K, V> slice, K key, V oldValue, V newValue);
}
