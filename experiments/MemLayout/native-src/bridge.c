#include <jni.h>
#include <dlfcn.h>
#include <stdint.h>
#include <stdio.h>

JNIEXPORT jlong JNICALL Java_bridge_findSym(JNIEnv *env, jobject bridge, jstring name) {
	char* nameChars = (*env)->GetStringUTFChars(env, name, NULL);
	jlong result = dlsym(RTLD_DEFAULT, nameChars);
	(*env)->ReleaseStringUTFChars(env, name, nameChars);
	return result;
}

JNIEXPORT jlong JNICALL Java_bridge_call0(JNIEnv *env, jobject obj, jlong ptr) {
	intptr_t (*func)() = ptr;
	return func();

}

JNIEXPORT jlong JNICALL Java_bridge_call1(JNIEnv *env, jobject obj, jlong ptr, jlong arg1) {
	intptr_t (*func)(intptr_t) = ptr;
	return func(arg1);

}

JNIEXPORT jlong JNICALL Java_bridge_call2(JNIEnv *env, jobject obj, jlong ptr, jlong arg1, jlong arg2) {
	intptr_t (*func)(intptr_t, intptr_t) = ptr;
	return func(arg1, arg2);

}

JNIEXPORT jlong JNICALL Java_bridge_call3(JNIEnv *env, jobject obj, jlong ptr, jlong arg1, jlong arg2, jlong arg3) {
	intptr_t (*func)(intptr_t, intptr_t, intptr_t) = ptr;
	return func(arg1, arg2, arg3);

}

JNIEXPORT jlong JNICALL Java_bridge_call4(JNIEnv *env, jobject obj, jlong ptr, jlong arg1, jlong arg2, jlong arg3, jlong arg4) {
	intptr_t (*func)(intptr_t, intptr_t, intptr_t, intptr_t) = ptr;
	return func(arg1, arg2, arg3, arg4);

}
