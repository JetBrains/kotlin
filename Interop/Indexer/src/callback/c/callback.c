#include <jni.h>
#include <stdlib.h>
#include <assert.h>

static JavaVM *vm = NULL;

// Returns the JNI env which can be used by the caller.
// If current thread is not attached to JVM, then it gets attached as daemon.
static JNIEnv* getCurrentEnv() {
    JNIEnv* env;
    assert (vm != NULL);
    jint res = (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_1);
    if (res != JNI_OK) {
        assert (res == JNI_EDETACHED);
        res = (*vm)->AttachCurrentThreadAsDaemon(vm, (void**)&env, NULL);
        assert (res == JNI_OK);
    }
    return env;
}

jint JNI_OnLoad(JavaVM *vm_, void *reserved) {
    vm = vm_;
    return JNI_VERSION_1_1;
}

// The implementation of native callback.
void indexDeclarationNativeCallback(void* arg1, void* arg2) {
    JNIEnv* env = getCurrentEnv();

    static jclass cls = NULL;
    static jmethodID entryFromNative = NULL;

    if (cls == NULL) {
        cls = (*env)->FindClass(env, "org/jetbrains/kotlin/native/interop/indexer/indexDeclarationCallback");
        assert(cls != NULL);
        entryFromNative = (*env)->GetStaticMethodID(env, cls, "entryFromNative", "(J)V");
        assert(entryFromNative != NULL);
    }
    
    (*env)->CallStaticVoidMethod(env, cls, entryFromNative, (jlong) arg2);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        assert(0);
    }
}

JNIEXPORT jlong JNICALL Java_org_jetbrains_kotlin_native_interop_indexer_indexDeclarationCallback_nativeCallbackPtr(JNIEnv *env, jobject obj) {
    return (jlong) indexDeclarationNativeCallback;
}
