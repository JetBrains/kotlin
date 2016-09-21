#!/bin/bash

clang -shared -olibcallback.dylib src/callback/c/callback.c -I /System/Library/Frameworks/JavaVM.framework/Headers -pthread

