/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("clang")
@file:Suppress("UNUSED_EXPRESSION", "UNUSED_VARIABLE")
package clang

import kotlinx.cinterop.*

fun asctime(arg0: CValuesRef<tm>?): CPointer<ByteVar>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_asctime(_arg0)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_asctime(arg0: NativePtr): NativePtr

fun clock(): clock_t {
    val res = kni_clock()
    return res
}

private external fun kni_clock(): Long

fun ctime(arg0: CValuesRef<time_tVar>?): CPointer<ByteVar>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_ctime(_arg0)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_ctime(arg0: NativePtr): NativePtr

fun difftime(arg0: time_t, arg1: time_t): Double {
    val _arg0 = arg0
    val _arg1 = arg1
    val res = kni_difftime(_arg0, _arg1)
    return res
}

private external fun kni_difftime(arg0: Long, arg1: Long): Double

fun getdate(arg0: String?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.cstr?.getPointer(memScope).rawValue
        val res = kni_getdate(_arg0)
        interpretCPointer<tm>(res)
    }
}

private external fun kni_getdate(arg0: NativePtr): NativePtr

fun gmtime(arg0: CValuesRef<time_tVar>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_gmtime(_arg0)
        interpretCPointer<tm>(res)
    }
}

private external fun kni_gmtime(arg0: NativePtr): NativePtr

fun localtime(arg0: CValuesRef<time_tVar>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_localtime(_arg0)
        interpretCPointer<tm>(res)
    }
}

private external fun kni_localtime(arg0: NativePtr): NativePtr

fun mktime(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_mktime(_arg0)
        res
    }
}

private external fun kni_mktime(arg0: NativePtr): Long

fun strftime(arg0: CValuesRef<ByteVar>?, arg1: size_t, arg2: String?, arg3: CValuesRef<tm>?): size_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1
        val _arg2 = arg2?.cstr?.getPointer(memScope).rawValue
        val _arg3 = arg3?.getPointer(memScope).rawValue
        val res = kni_strftime(_arg0, _arg1, _arg2, _arg3)
        res
    }
}

private external fun kni_strftime(arg0: NativePtr, arg1: Long, arg2: NativePtr, arg3: NativePtr): Long

fun strptime(arg0: String?, arg1: String?, arg2: CValuesRef<tm>?): CPointer<ByteVar>? {
    return memScoped {
        val _arg0 = arg0?.cstr?.getPointer(memScope).rawValue
        val _arg1 = arg1?.cstr?.getPointer(memScope).rawValue
        val _arg2 = arg2?.getPointer(memScope).rawValue
        val res = kni_strptime(_arg0, _arg1, _arg2)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_strptime(arg0: NativePtr, arg1: NativePtr, arg2: NativePtr): NativePtr

fun time(arg0: CValuesRef<time_tVar>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_time(_arg0)
        res
    }
}

private external fun kni_time(arg0: NativePtr): Long

fun tzset(): Unit {
    val res = kni_tzset()
    return res
}

private external fun kni_tzset(): Unit

fun asctime_r(arg0: CValuesRef<tm>?, arg1: CValuesRef<ByteVar>?): CPointer<ByteVar>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = kni_asctime_r(_arg0, _arg1)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_asctime_r(arg0: NativePtr, arg1: NativePtr): NativePtr

fun ctime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<ByteVar>?): CPointer<ByteVar>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = kni_ctime_r(_arg0, _arg1)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_ctime_r(arg0: NativePtr, arg1: NativePtr): NativePtr

fun gmtime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<tm>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = kni_gmtime_r(_arg0, _arg1)
        interpretCPointer<tm>(res)
    }
}

private external fun kni_gmtime_r(arg0: NativePtr, arg1: NativePtr): NativePtr

fun localtime_r(arg0: CValuesRef<time_tVar>?, arg1: CValuesRef<tm>?): CPointer<tm>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1?.getPointer(memScope).rawValue
        val res = kni_localtime_r(_arg0, _arg1)
        interpretCPointer<tm>(res)
    }
}

private external fun kni_localtime_r(arg0: NativePtr, arg1: NativePtr): NativePtr

fun posix2time(arg0: time_t): time_t {
    val _arg0 = arg0
    val res = kni_posix2time(_arg0)
    return res
}

private external fun kni_posix2time(arg0: Long): Long

fun tzsetwall(): Unit {
    val res = kni_tzsetwall()
    return res
}

private external fun kni_tzsetwall(): Unit

fun time2posix(arg0: time_t): time_t {
    val _arg0 = arg0
    val res = kni_time2posix(_arg0)
    return res
}

private external fun kni_time2posix(arg0: Long): Long

fun timelocal(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_timelocal(_arg0)
        res
    }
}

private external fun kni_timelocal(arg0: NativePtr): Long

fun timegm(arg0: CValuesRef<tm>?): time_t {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_timegm(_arg0)
        res
    }
}

private external fun kni_timegm(arg0: NativePtr): Long

fun nanosleep(__rqtp: CValuesRef<timespec>?, __rmtp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___rqtp = __rqtp?.getPointer(memScope).rawValue
        val ___rmtp = __rmtp?.getPointer(memScope).rawValue
        val res = kni_nanosleep(___rqtp, ___rmtp)
        res
    }
}

private external fun kni_nanosleep(__rqtp: NativePtr, __rmtp: NativePtr): Int

fun clock_getres(__clock_id: clockid_t, __res: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___res = __res?.getPointer(memScope).rawValue
        val res = kni_clock_getres(___clock_id, ___res)
        res
    }
}

private external fun kni_clock_getres(__clock_id: Int, __res: NativePtr): Int

fun clock_gettime(__clock_id: clockid_t, __tp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___tp = __tp?.getPointer(memScope).rawValue
        val res = kni_clock_gettime(___clock_id, ___tp)
        res
    }
}

private external fun kni_clock_gettime(__clock_id: Int, __tp: NativePtr): Int

fun clock_gettime_nsec_np(__clock_id: clockid_t): __uint64_t {
    val ___clock_id = __clock_id
    val res = kni_clock_gettime_nsec_np(___clock_id)
    return res
}

private external fun kni_clock_gettime_nsec_np(__clock_id: Int): Long

fun clock_settime(__clock_id: clockid_t, __tp: CValuesRef<timespec>?): Int {
    return memScoped {
        val ___clock_id = __clock_id
        val ___tp = __tp?.getPointer(memScope).rawValue
        val res = kni_clock_settime(___clock_id, ___tp)
        res
    }
}

private external fun kni_clock_settime(__clock_id: Int, __tp: NativePtr): Int

fun clang_getCString(string: CValue<CXString>): CPointer<ByteVar>? {
    return memScoped {
        val _string = string.getPointer(memScope).rawValue
        val res = kni_clang_getCString(_string)
        interpretCPointer<ByteVar>(res)
    }
}

private external fun kni_clang_getCString(string: NativePtr): NativePtr

fun clang_disposeString(string: CValue<CXString>): Unit {
    return memScoped {
        val _string = string.getPointer(memScope).rawValue
        val res = kni_clang_disposeString(_string)
        res
    }
}

private external fun kni_clang_disposeString(string: NativePtr): Unit

fun clang_disposeStringSet(set: CValuesRef<CXStringSet>?): Unit {
    return memScoped {
        val _set = set?.getPointer(memScope).rawValue
        val res = kni_clang_disposeStringSet(_set)
        res
    }
}

private external fun kni_clang_disposeStringSet(set: NativePtr): Unit

fun clang_getBuildSessionTimestamp(): Long {
    val res = kni_clang_getBuildSessionTimestamp()
    return res
}

private external fun kni_clang_getBuildSessionTimestamp(): Long

fun clang_VirtualFileOverlay_create(options: Int): CXVirtualFileOverlay? {
    val _options = options
    val res = kni_clang_VirtualFileOverlay_create(_options)
    return interpretCPointer<CXVirtualFileOverlayImpl>(res)
}

private external fun kni_clang_VirtualFileOverlay_create(options: Int): NativePtr

fun clang_VirtualFileOverlay_addFileMapping(arg0: CXVirtualFileOverlay?, virtualPath: String?, realPath: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _virtualPath = virtualPath?.cstr?.getPointer(memScope).rawValue
        val _realPath = realPath?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_VirtualFileOverlay_addFileMapping(_arg0, _virtualPath, _realPath)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_VirtualFileOverlay_addFileMapping(arg0: NativePtr, virtualPath: NativePtr, realPath: NativePtr): Int

fun clang_VirtualFileOverlay_setCaseSensitivity(arg0: CXVirtualFileOverlay?, caseSensitive: Int): CXErrorCode {
    val _arg0 = arg0.rawValue
    val _caseSensitive = caseSensitive
    val res = kni_clang_VirtualFileOverlay_setCaseSensitivity(_arg0, _caseSensitive)
    return CXErrorCode.byValue(res)
}

private external fun kni_clang_VirtualFileOverlay_setCaseSensitivity(arg0: NativePtr, caseSensitive: Int): Int

fun clang_VirtualFileOverlay_writeToBuffer(arg0: CXVirtualFileOverlay?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<ByteVar>>?, out_buffer_size: CValuesRef<IntVar>?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _options = options
        val _out_buffer_ptr = out_buffer_ptr?.getPointer(memScope).rawValue
        val _out_buffer_size = out_buffer_size?.getPointer(memScope).rawValue
        val res = kni_clang_VirtualFileOverlay_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_VirtualFileOverlay_writeToBuffer(arg0: NativePtr, options: Int, out_buffer_ptr: NativePtr, out_buffer_size: NativePtr): Int

fun clang_free(buffer: COpaquePointer?): Unit {
    val _buffer = buffer.rawValue
    val res = kni_clang_free(_buffer)
    return res
}

private external fun kni_clang_free(buffer: NativePtr): Unit

fun clang_VirtualFileOverlay_dispose(arg0: CXVirtualFileOverlay?): Unit {
    val _arg0 = arg0.rawValue
    val res = kni_clang_VirtualFileOverlay_dispose(_arg0)
    return res
}

private external fun kni_clang_VirtualFileOverlay_dispose(arg0: NativePtr): Unit

fun clang_ModuleMapDescriptor_create(options: Int): CXModuleMapDescriptor? {
    val _options = options
    val res = kni_clang_ModuleMapDescriptor_create(_options)
    return interpretCPointer<CXModuleMapDescriptorImpl>(res)
}

private external fun kni_clang_ModuleMapDescriptor_create(options: Int): NativePtr

fun clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_ModuleMapDescriptor_setFrameworkModuleName(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_ModuleMapDescriptor_setFrameworkModuleName(arg0: NativePtr, name: NativePtr): Int

fun clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: CXModuleMapDescriptor?, name: String?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_ModuleMapDescriptor_setUmbrellaHeader(_arg0, _name)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_ModuleMapDescriptor_setUmbrellaHeader(arg0: NativePtr, name: NativePtr): Int

fun clang_ModuleMapDescriptor_writeToBuffer(arg0: CXModuleMapDescriptor?, options: Int, out_buffer_ptr: CValuesRef<CPointerVar<ByteVar>>?, out_buffer_size: CValuesRef<IntVar>?): CXErrorCode {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _options = options
        val _out_buffer_ptr = out_buffer_ptr?.getPointer(memScope).rawValue
        val _out_buffer_size = out_buffer_size?.getPointer(memScope).rawValue
        val res = kni_clang_ModuleMapDescriptor_writeToBuffer(_arg0, _options, _out_buffer_ptr, _out_buffer_size)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_ModuleMapDescriptor_writeToBuffer(arg0: NativePtr, options: Int, out_buffer_ptr: NativePtr, out_buffer_size: NativePtr): Int

fun clang_ModuleMapDescriptor_dispose(arg0: CXModuleMapDescriptor?): Unit {
    val _arg0 = arg0.rawValue
    val res = kni_clang_ModuleMapDescriptor_dispose(_arg0)
    return res
}

private external fun kni_clang_ModuleMapDescriptor_dispose(arg0: NativePtr): Unit

fun clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): CXIndex? {
    val _excludeDeclarationsFromPCH = excludeDeclarationsFromPCH
    val _displayDiagnostics = displayDiagnostics
    val res = kni_clang_createIndex(_excludeDeclarationsFromPCH, _displayDiagnostics)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_createIndex(excludeDeclarationsFromPCH: Int, displayDiagnostics: Int): NativePtr

fun clang_disposeIndex(index: CXIndex?): Unit {
    val _index = index.rawValue
    val res = kni_clang_disposeIndex(_index)
    return res
}

private external fun kni_clang_disposeIndex(index: NativePtr): Unit

fun clang_CXIndex_setGlobalOptions(arg0: CXIndex?, options: Int): Unit {
    val _arg0 = arg0.rawValue
    val _options = options
    val res = kni_clang_CXIndex_setGlobalOptions(_arg0, _options)
    return res
}

private external fun kni_clang_CXIndex_setGlobalOptions(arg0: NativePtr, options: Int): Unit

fun clang_CXIndex_getGlobalOptions(arg0: CXIndex?): Int {
    val _arg0 = arg0.rawValue
    val res = kni_clang_CXIndex_getGlobalOptions(_arg0)
    return res
}

private external fun kni_clang_CXIndex_getGlobalOptions(arg0: NativePtr): Int

fun clang_getFileName(SFile: CXFile?): CValue<CXString> {
    return memScoped {
        val _SFile = SFile.rawValue
        val res = kni_clang_getFileName(_SFile, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getFileName(SFile: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getFileTime(SFile: CXFile?): time_t {
    val _SFile = SFile.rawValue
    val res = kni_clang_getFileTime(_SFile)
    return res
}

private external fun kni_clang_getFileTime(SFile: NativePtr): Long

fun clang_getFileUniqueID(file: CXFile?, outID: CValuesRef<CXFileUniqueID>?): Int {
    return memScoped {
        val _file = file.rawValue
        val _outID = outID?.getPointer(memScope).rawValue
        val res = kni_clang_getFileUniqueID(_file, _outID)
        res
    }
}

private external fun kni_clang_getFileUniqueID(file: NativePtr, outID: NativePtr): Int

fun clang_isFileMultipleIncludeGuarded(tu: CXTranslationUnit?, file: CXFile?): Int {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val res = kni_clang_isFileMultipleIncludeGuarded(_tu, _file)
    return res
}

private external fun kni_clang_isFileMultipleIncludeGuarded(tu: NativePtr, file: NativePtr): Int

fun clang_getFile(tu: CXTranslationUnit?, file_name: String?): CXFile? {
    return memScoped {
        val _tu = tu.rawValue
        val _file_name = file_name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_getFile(_tu, _file_name)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_getFile(tu: NativePtr, file_name: NativePtr): NativePtr

fun clang_File_isEqual(file1: CXFile?, file2: CXFile?): Int {
    val _file1 = file1.rawValue
    val _file2 = file2.rawValue
    val res = kni_clang_File_isEqual(_file1, _file2)
    return res
}

private external fun kni_clang_File_isEqual(file1: NativePtr, file2: NativePtr): Int

fun clang_getNullLocation(): CValue<CXSourceLocation> {
    return memScoped {
        val res = kni_clang_getNullLocation(alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getNullLocation(retValPlacement: NativePtr): NativePtr

fun clang_equalLocations(loc1: CValue<CXSourceLocation>, loc2: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _loc1 = loc1.getPointer(memScope).rawValue
        val _loc2 = loc2.getPointer(memScope).rawValue
        val res = kni_clang_equalLocations(_loc1, _loc2)
        res
    }
}

private external fun kni_clang_equalLocations(loc1: NativePtr, loc2: NativePtr): Int

fun clang_getLocation(tu: CXTranslationUnit?, file: CXFile?, line: Int, column: Int): CValue<CXSourceLocation> {
    return memScoped {
        val _tu = tu.rawValue
        val _file = file.rawValue
        val _line = line
        val _column = column
        val res = kni_clang_getLocation(_tu, _file, _line, _column, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getLocation(tu: NativePtr, file: NativePtr, line: Int, column: Int, retValPlacement: NativePtr): NativePtr

fun clang_getLocationForOffset(tu: CXTranslationUnit?, file: CXFile?, offset: Int): CValue<CXSourceLocation> {
    return memScoped {
        val _tu = tu.rawValue
        val _file = file.rawValue
        val _offset = offset
        val res = kni_clang_getLocationForOffset(_tu, _file, _offset, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getLocationForOffset(tu: NativePtr, file: NativePtr, offset: Int, retValPlacement: NativePtr): NativePtr

fun clang_Location_isInSystemHeader(location: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val res = kni_clang_Location_isInSystemHeader(_location)
        res
    }
}

private external fun kni_clang_Location_isInSystemHeader(location: NativePtr): Int

fun clang_Location_isFromMainFile(location: CValue<CXSourceLocation>): Int {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val res = kni_clang_Location_isFromMainFile(_location)
        res
    }
}

private external fun kni_clang_Location_isFromMainFile(location: NativePtr): Int

fun clang_getNullRange(): CValue<CXSourceRange> {
    return memScoped {
        val res = kni_clang_getNullRange(alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getNullRange(retValPlacement: NativePtr): NativePtr

fun clang_getRange(begin: CValue<CXSourceLocation>, end: CValue<CXSourceLocation>): CValue<CXSourceRange> {
    return memScoped {
        val _begin = begin.getPointer(memScope).rawValue
        val _end = end.getPointer(memScope).rawValue
        val res = kni_clang_getRange(_begin, _end, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getRange(begin: NativePtr, end: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_equalRanges(range1: CValue<CXSourceRange>, range2: CValue<CXSourceRange>): Int {
    return memScoped {
        val _range1 = range1.getPointer(memScope).rawValue
        val _range2 = range2.getPointer(memScope).rawValue
        val res = kni_clang_equalRanges(_range1, _range2)
        res
    }
}

private external fun kni_clang_equalRanges(range1: NativePtr, range2: NativePtr): Int

fun clang_Range_isNull(range: CValue<CXSourceRange>): Int {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = kni_clang_Range_isNull(_range)
        res
    }
}

private external fun kni_clang_Range_isNull(range: NativePtr): Int

fun clang_getExpansionLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = kni_clang_getExpansionLocation(_location, _file, _line, _column, _offset)
        res
    }
}

private external fun kni_clang_getExpansionLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit

fun clang_getPresumedLocation(location: CValue<CXSourceLocation>, filename: CValuesRef<CXString>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _filename = filename?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val res = kni_clang_getPresumedLocation(_location, _filename, _line, _column)
        res
    }
}

private external fun kni_clang_getPresumedLocation(location: NativePtr, filename: NativePtr, line: NativePtr, column: NativePtr): Unit

fun clang_getInstantiationLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = kni_clang_getInstantiationLocation(_location, _file, _line, _column, _offset)
        res
    }
}

private external fun kni_clang_getInstantiationLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit

fun clang_getSpellingLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = kni_clang_getSpellingLocation(_location, _file, _line, _column, _offset)
        res
    }
}

private external fun kni_clang_getSpellingLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit

fun clang_getFileLocation(location: CValue<CXSourceLocation>, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _location = location.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = kni_clang_getFileLocation(_location, _file, _line, _column, _offset)
        res
    }
}

private external fun kni_clang_getFileLocation(location: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit

fun clang_getRangeStart(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = kni_clang_getRangeStart(_range, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getRangeStart(range: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getRangeEnd(range: CValue<CXSourceRange>): CValue<CXSourceLocation> {
    return memScoped {
        val _range = range.getPointer(memScope).rawValue
        val res = kni_clang_getRangeEnd(_range, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getRangeEnd(range: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getSkippedRanges(tu: CXTranslationUnit?, file: CXFile?): CPointer<CXSourceRangeList>? {
    val _tu = tu.rawValue
    val _file = file.rawValue
    val res = kni_clang_getSkippedRanges(_tu, _file)
    return interpretCPointer<CXSourceRangeList>(res)
}

private external fun kni_clang_getSkippedRanges(tu: NativePtr, file: NativePtr): NativePtr

fun clang_disposeSourceRangeList(ranges: CValuesRef<CXSourceRangeList>?): Unit {
    return memScoped {
        val _ranges = ranges?.getPointer(memScope).rawValue
        val res = kni_clang_disposeSourceRangeList(_ranges)
        res
    }
}

private external fun kni_clang_disposeSourceRangeList(ranges: NativePtr): Unit

fun clang_getNumDiagnosticsInSet(Diags: CXDiagnosticSet?): Int {
    val _Diags = Diags.rawValue
    val res = kni_clang_getNumDiagnosticsInSet(_Diags)
    return res
}

private external fun kni_clang_getNumDiagnosticsInSet(Diags: NativePtr): Int

fun clang_getDiagnosticInSet(Diags: CXDiagnosticSet?, Index: Int): CXDiagnostic? {
    val _Diags = Diags.rawValue
    val _Index = Index
    val res = kni_clang_getDiagnosticInSet(_Diags, _Index)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getDiagnosticInSet(Diags: NativePtr, Index: Int): NativePtr

fun clang_loadDiagnostics(file: String?, error: CValuesRef<CXLoadDiag_Error.Var>?, errorString: CValuesRef<CXString>?): CXDiagnosticSet? {
    return memScoped {
        val _file = file?.cstr?.getPointer(memScope).rawValue
        val _error = error?.getPointer(memScope).rawValue
        val _errorString = errorString?.getPointer(memScope).rawValue
        val res = kni_clang_loadDiagnostics(_file, _error, _errorString)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_loadDiagnostics(file: NativePtr, error: NativePtr, errorString: NativePtr): NativePtr

fun clang_disposeDiagnosticSet(Diags: CXDiagnosticSet?): Unit {
    val _Diags = Diags.rawValue
    val res = kni_clang_disposeDiagnosticSet(_Diags)
    return res
}

private external fun kni_clang_disposeDiagnosticSet(Diags: NativePtr): Unit

fun clang_getChildDiagnostics(D: CXDiagnostic?): CXDiagnosticSet? {
    val _D = D.rawValue
    val res = kni_clang_getChildDiagnostics(_D)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getChildDiagnostics(D: NativePtr): NativePtr

fun clang_getNumDiagnostics(Unit: CXTranslationUnit?): Int {
    val _Unit = Unit.rawValue
    val res = kni_clang_getNumDiagnostics(_Unit)
    return res
}

private external fun kni_clang_getNumDiagnostics(Unit: NativePtr): Int

fun clang_getDiagnostic(Unit: CXTranslationUnit?, Index: Int): CXDiagnostic? {
    val _Unit = Unit.rawValue
    val _Index = Index
    val res = kni_clang_getDiagnostic(_Unit, _Index)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getDiagnostic(Unit: NativePtr, Index: Int): NativePtr

fun clang_getDiagnosticSetFromTU(Unit: CXTranslationUnit?): CXDiagnosticSet? {
    val _Unit = Unit.rawValue
    val res = kni_clang_getDiagnosticSetFromTU(_Unit)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getDiagnosticSetFromTU(Unit: NativePtr): NativePtr

fun clang_disposeDiagnostic(Diagnostic: CXDiagnostic?): Unit {
    val _Diagnostic = Diagnostic.rawValue
    val res = kni_clang_disposeDiagnostic(_Diagnostic)
    return res
}

private external fun kni_clang_disposeDiagnostic(Diagnostic: NativePtr): Unit

fun clang_formatDiagnostic(Diagnostic: CXDiagnostic?, Options: Int): CValue<CXString> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _Options = Options
        val res = kni_clang_formatDiagnostic(_Diagnostic, _Options, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_formatDiagnostic(Diagnostic: NativePtr, Options: Int, retValPlacement: NativePtr): NativePtr

fun clang_defaultDiagnosticDisplayOptions(): Int {
    val res = kni_clang_defaultDiagnosticDisplayOptions()
    return res
}

private external fun kni_clang_defaultDiagnosticDisplayOptions(): Int

fun clang_getDiagnosticSeverity(arg0: CXDiagnostic?): CXDiagnosticSeverity {
    val _arg0 = arg0.rawValue
    val res = kni_clang_getDiagnosticSeverity(_arg0)
    return CXDiagnosticSeverity.byValue(res)
}

private external fun kni_clang_getDiagnosticSeverity(arg0: NativePtr): Int

fun clang_getDiagnosticLocation(arg0: CXDiagnostic?): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = kni_clang_getDiagnosticLocation(_arg0, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticLocation(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticSpelling(arg0: CXDiagnostic?): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = kni_clang_getDiagnosticSpelling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticSpelling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticOption(Diag: CXDiagnostic?, Disable: CValuesRef<CXString>?): CValue<CXString> {
    return memScoped {
        val _Diag = Diag.rawValue
        val _Disable = Disable?.getPointer(memScope).rawValue
        val res = kni_clang_getDiagnosticOption(_Diag, _Disable, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticOption(Diag: NativePtr, Disable: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticCategory(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = kni_clang_getDiagnosticCategory(_arg0)
    return res
}

private external fun kni_clang_getDiagnosticCategory(arg0: NativePtr): Int

fun clang_getDiagnosticCategoryName(Category: Int): CValue<CXString> {
    return memScoped {
        val _Category = Category
        val res = kni_clang_getDiagnosticCategoryName(_Category, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticCategoryName(Category: Int, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticCategoryText(arg0: CXDiagnostic?): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = kni_clang_getDiagnosticCategoryText(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticCategoryText(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticNumRanges(arg0: CXDiagnostic?): Int {
    val _arg0 = arg0.rawValue
    val res = kni_clang_getDiagnosticNumRanges(_arg0)
    return res
}

private external fun kni_clang_getDiagnosticNumRanges(arg0: NativePtr): Int

fun clang_getDiagnosticRange(Diagnostic: CXDiagnostic?, Range: Int): CValue<CXSourceRange> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _Range = Range
        val res = kni_clang_getDiagnosticRange(_Diagnostic, _Range, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticRange(Diagnostic: NativePtr, Range: Int, retValPlacement: NativePtr): NativePtr

fun clang_getDiagnosticNumFixIts(Diagnostic: CXDiagnostic?): Int {
    val _Diagnostic = Diagnostic.rawValue
    val res = kni_clang_getDiagnosticNumFixIts(_Diagnostic)
    return res
}

private external fun kni_clang_getDiagnosticNumFixIts(Diagnostic: NativePtr): Int

fun clang_getDiagnosticFixIt(Diagnostic: CXDiagnostic?, FixIt: Int, ReplacementRange: CValuesRef<CXSourceRange>?): CValue<CXString> {
    return memScoped {
        val _Diagnostic = Diagnostic.rawValue
        val _FixIt = FixIt
        val _ReplacementRange = ReplacementRange?.getPointer(memScope).rawValue
        val res = kni_clang_getDiagnosticFixIt(_Diagnostic, _FixIt, _ReplacementRange, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDiagnosticFixIt(Diagnostic: NativePtr, FixIt: Int, ReplacementRange: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTranslationUnitSpelling(CTUnit: CXTranslationUnit?): CValue<CXString> {
    return memScoped {
        val _CTUnit = CTUnit.rawValue
        val res = kni_clang_getTranslationUnitSpelling(_CTUnit, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getTranslationUnitSpelling(CTUnit: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_createTranslationUnitFromSourceFile(CIdx: CXIndex?, source_filename: String?, num_clang_command_line_args: Int, clang_command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _num_clang_command_line_args = num_clang_command_line_args
        val _clang_command_line_args = clang_command_line_args?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val res = kni_clang_createTranslationUnitFromSourceFile(_CIdx, _source_filename, _num_clang_command_line_args, _clang_command_line_args, _num_unsaved_files, _unsaved_files)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

private external fun kni_clang_createTranslationUnitFromSourceFile(CIdx: NativePtr, source_filename: NativePtr, num_clang_command_line_args: Int, clang_command_line_args: NativePtr, num_unsaved_files: Int, unsaved_files: NativePtr): NativePtr

fun clang_createTranslationUnit(CIdx: CXIndex?, ast_filename: String?): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_createTranslationUnit(_CIdx, _ast_filename)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

private external fun kni_clang_createTranslationUnit(CIdx: NativePtr, ast_filename: NativePtr): NativePtr

fun clang_createTranslationUnit2(CIdx: CXIndex?, ast_filename: String?, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _ast_filename = ast_filename?.cstr?.getPointer(memScope).rawValue
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val res = kni_clang_createTranslationUnit2(_CIdx, _ast_filename, _out_TU)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_createTranslationUnit2(CIdx: NativePtr, ast_filename: NativePtr, out_TU: NativePtr): Int

fun clang_defaultEditingTranslationUnitOptions(): Int {
    val res = kni_clang_defaultEditingTranslationUnitOptions()
    return res
}

private external fun kni_clang_defaultEditingTranslationUnitOptions(): Int

fun clang_parseTranslationUnit(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CXTranslationUnit? {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = kni_clang_parseTranslationUnit(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

private external fun kni_clang_parseTranslationUnit(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int): NativePtr

fun clang_parseTranslationUnit2(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val res = kni_clang_parseTranslationUnit2(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options, _out_TU)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_parseTranslationUnit2(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int, out_TU: NativePtr): Int

fun clang_parseTranslationUnit2FullArgv(CIdx: CXIndex?, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int, out_TU: CValuesRef<CXTranslationUnitVar>?): CXErrorCode {
    return memScoped {
        val _CIdx = CIdx.rawValue
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val res = kni_clang_parseTranslationUnit2FullArgv(_CIdx, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _options, _out_TU)
        CXErrorCode.byValue(res)
    }
}

private external fun kni_clang_parseTranslationUnit2FullArgv(CIdx: NativePtr, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int, out_TU: NativePtr): Int

fun clang_defaultSaveOptions(TU: CXTranslationUnit?): Int {
    val _TU = TU.rawValue
    val res = kni_clang_defaultSaveOptions(_TU)
    return res
}

private external fun kni_clang_defaultSaveOptions(TU: NativePtr): Int

fun clang_saveTranslationUnit(TU: CXTranslationUnit?, FileName: String?, options: Int): Int {
    return memScoped {
        val _TU = TU.rawValue
        val _FileName = FileName?.cstr?.getPointer(memScope).rawValue
        val _options = options
        val res = kni_clang_saveTranslationUnit(_TU, _FileName, _options)
        res
    }
}

private external fun kni_clang_saveTranslationUnit(TU: NativePtr, FileName: NativePtr, options: Int): Int

fun clang_disposeTranslationUnit(arg0: CXTranslationUnit?): Unit {
    val _arg0 = arg0.rawValue
    val res = kni_clang_disposeTranslationUnit(_arg0)
    return res
}

private external fun kni_clang_disposeTranslationUnit(arg0: NativePtr): Unit

fun clang_defaultReparseOptions(TU: CXTranslationUnit?): Int {
    val _TU = TU.rawValue
    val res = kni_clang_defaultReparseOptions(_TU)
    return res
}

private external fun kni_clang_defaultReparseOptions(TU: NativePtr): Int

fun clang_reparseTranslationUnit(TU: CXTranslationUnit?, num_unsaved_files: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, options: Int): Int {
    return memScoped {
        val _TU = TU.rawValue
        val _num_unsaved_files = num_unsaved_files
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _options = options
        val res = kni_clang_reparseTranslationUnit(_TU, _num_unsaved_files, _unsaved_files, _options)
        res
    }
}

private external fun kni_clang_reparseTranslationUnit(TU: NativePtr, num_unsaved_files: Int, unsaved_files: NativePtr, options: Int): Int

fun clang_getTUResourceUsageName(kind: CXTUResourceUsageKind): CPointer<ByteVar>? {
    val _kind = kind.value
    val res = kni_clang_getTUResourceUsageName(_kind)
    return interpretCPointer<ByteVar>(res)
}

private external fun kni_clang_getTUResourceUsageName(kind: Int): NativePtr

fun clang_getCXTUResourceUsage(TU: CXTranslationUnit?): CValue<CXTUResourceUsage> {
    return memScoped {
        val _TU = TU.rawValue
        val res = kni_clang_getCXTUResourceUsage(_TU, alloc<CXTUResourceUsage>().rawPtr)
        interpretPointed<CXTUResourceUsage>(res).readValue()
    }
}

private external fun kni_clang_getCXTUResourceUsage(TU: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_disposeCXTUResourceUsage(usage: CValue<CXTUResourceUsage>): Unit {
    return memScoped {
        val _usage = usage.getPointer(memScope).rawValue
        val res = kni_clang_disposeCXTUResourceUsage(_usage)
        res
    }
}

private external fun kni_clang_disposeCXTUResourceUsage(usage: NativePtr): Unit

fun clang_getNullCursor(): CValue<CXCursor> {
    return memScoped {
        val res = kni_clang_getNullCursor(alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getNullCursor(retValPlacement: NativePtr): NativePtr

fun clang_getTranslationUnitCursor(arg0: CXTranslationUnit?): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val res = kni_clang_getTranslationUnitCursor(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getTranslationUnitCursor(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_equalCursors(arg0: CValue<CXCursor>, arg1: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = kni_clang_equalCursors(_arg0, _arg1)
        res
    }
}

private external fun kni_clang_equalCursors(arg0: NativePtr, arg1: NativePtr): Int

fun clang_Cursor_isNull(cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isNull(_cursor)
        res
    }
}

private external fun kni_clang_Cursor_isNull(cursor: NativePtr): Int

fun clang_hashCursor(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_hashCursor(_arg0)
        res
    }
}

private external fun kni_clang_hashCursor(arg0: NativePtr): Int

fun clang_getCursorKind(arg0: CValue<CXCursor>): CXCursorKind {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorKind(_arg0)
        CXCursorKind.byValue(res)
    }
}

private external fun kni_clang_getCursorKind(arg0: NativePtr): Int

fun clang_isDeclaration(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isDeclaration(_arg0)
    return res
}

private external fun kni_clang_isDeclaration(arg0: Int): Int

fun clang_isReference(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isReference(_arg0)
    return res
}

private external fun kni_clang_isReference(arg0: Int): Int

fun clang_isExpression(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isExpression(_arg0)
    return res
}

private external fun kni_clang_isExpression(arg0: Int): Int

fun clang_isStatement(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isStatement(_arg0)
    return res
}

private external fun kni_clang_isStatement(arg0: Int): Int

fun clang_isAttribute(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isAttribute(_arg0)
    return res
}

private external fun kni_clang_isAttribute(arg0: Int): Int

fun clang_Cursor_hasAttrs(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_hasAttrs(_C)
        res
    }
}

private external fun kni_clang_Cursor_hasAttrs(C: NativePtr): Int

fun clang_isInvalid(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isInvalid(_arg0)
    return res
}

private external fun kni_clang_isInvalid(arg0: Int): Int

fun clang_isTranslationUnit(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isTranslationUnit(_arg0)
    return res
}

private external fun kni_clang_isTranslationUnit(arg0: Int): Int

fun clang_isPreprocessing(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isPreprocessing(_arg0)
    return res
}

private external fun kni_clang_isPreprocessing(arg0: Int): Int

fun clang_isUnexposed(arg0: CXCursorKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_isUnexposed(_arg0)
    return res
}

private external fun kni_clang_isUnexposed(arg0: Int): Int

fun clang_getCursorLinkage(cursor: CValue<CXCursor>): CXLinkageKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorLinkage(_cursor)
        CXLinkageKind.byValue(res)
    }
}

private external fun kni_clang_getCursorLinkage(cursor: NativePtr): Int

fun clang_getCursorVisibility(cursor: CValue<CXCursor>): CXVisibilityKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorVisibility(_cursor)
        CXVisibilityKind.byValue(res)
    }
}

private external fun kni_clang_getCursorVisibility(cursor: NativePtr): Int

fun clang_getCursorAvailability(cursor: CValue<CXCursor>): CXAvailabilityKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorAvailability(_cursor)
        CXAvailabilityKind.byValue(res)
    }
}

private external fun kni_clang_getCursorAvailability(cursor: NativePtr): Int

fun clang_getCursorPlatformAvailability(cursor: CValue<CXCursor>, always_deprecated: CValuesRef<IntVar>?, deprecated_message: CValuesRef<CXString>?, always_unavailable: CValuesRef<IntVar>?, unavailable_message: CValuesRef<CXString>?, availability: CValuesRef<CXPlatformAvailability>?, availability_size: Int): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _always_deprecated = always_deprecated?.getPointer(memScope).rawValue
        val _deprecated_message = deprecated_message?.getPointer(memScope).rawValue
        val _always_unavailable = always_unavailable?.getPointer(memScope).rawValue
        val _unavailable_message = unavailable_message?.getPointer(memScope).rawValue
        val _availability = availability?.getPointer(memScope).rawValue
        val _availability_size = availability_size
        val res = kni_clang_getCursorPlatformAvailability(_cursor, _always_deprecated, _deprecated_message, _always_unavailable, _unavailable_message, _availability, _availability_size)
        res
    }
}

private external fun kni_clang_getCursorPlatformAvailability(cursor: NativePtr, always_deprecated: NativePtr, deprecated_message: NativePtr, always_unavailable: NativePtr, unavailable_message: NativePtr, availability: NativePtr, availability_size: Int): Int

fun clang_disposeCXPlatformAvailability(availability: CValuesRef<CXPlatformAvailability>?): Unit {
    return memScoped {
        val _availability = availability?.getPointer(memScope).rawValue
        val res = kni_clang_disposeCXPlatformAvailability(_availability)
        res
    }
}

private external fun kni_clang_disposeCXPlatformAvailability(availability: NativePtr): Unit

fun clang_getCursorLanguage(cursor: CValue<CXCursor>): CXLanguageKind {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorLanguage(_cursor)
        CXLanguageKind.byValue(res)
    }
}

private external fun kni_clang_getCursorLanguage(cursor: NativePtr): Int

fun clang_Cursor_getTranslationUnit(arg0: CValue<CXCursor>): CXTranslationUnit? {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getTranslationUnit(_arg0)
        interpretCPointer<CXTranslationUnitImpl>(res)
    }
}

private external fun kni_clang_Cursor_getTranslationUnit(arg0: NativePtr): NativePtr

fun clang_createCXCursorSet(): CXCursorSet? {
    val res = kni_clang_createCXCursorSet()
    return interpretCPointer<CXCursorSetImpl>(res)
}

private external fun kni_clang_createCXCursorSet(): NativePtr

fun clang_disposeCXCursorSet(cset: CXCursorSet?): Unit {
    val _cset = cset.rawValue
    val res = kni_clang_disposeCXCursorSet(_cset)
    return res
}

private external fun kni_clang_disposeCXCursorSet(cset: NativePtr): Unit

fun clang_CXCursorSet_contains(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cset = cset.rawValue
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_CXCursorSet_contains(_cset, _cursor)
        res
    }
}

private external fun kni_clang_CXCursorSet_contains(cset: NativePtr, cursor: NativePtr): Int

fun clang_CXCursorSet_insert(cset: CXCursorSet?, cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cset = cset.rawValue
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_CXCursorSet_insert(_cset, _cursor)
        res
    }
}

private external fun kni_clang_CXCursorSet_insert(cset: NativePtr, cursor: NativePtr): Int

fun clang_getCursorSemanticParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorSemanticParent(_cursor, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCursorSemanticParent(cursor: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorLexicalParent(cursor: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorLexicalParent(_cursor, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCursorLexicalParent(cursor: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getOverriddenCursors(cursor: CValue<CXCursor>, overridden: CValuesRef<CPointerVar<CXCursor>>?, num_overridden: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _overridden = overridden?.getPointer(memScope).rawValue
        val _num_overridden = num_overridden?.getPointer(memScope).rawValue
        val res = kni_clang_getOverriddenCursors(_cursor, _overridden, _num_overridden)
        res
    }
}

private external fun kni_clang_getOverriddenCursors(cursor: NativePtr, overridden: NativePtr, num_overridden: NativePtr): Unit

fun clang_disposeOverriddenCursors(overridden: CValuesRef<CXCursor>?): Unit {
    return memScoped {
        val _overridden = overridden?.getPointer(memScope).rawValue
        val res = kni_clang_disposeOverriddenCursors(_overridden)
        res
    }
}

private external fun kni_clang_disposeOverriddenCursors(overridden: NativePtr): Unit

fun clang_getIncludedFile(cursor: CValue<CXCursor>): CXFile? {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getIncludedFile(_cursor)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_getIncludedFile(cursor: NativePtr): NativePtr

fun clang_getCursor(arg0: CXTranslationUnit?, arg1: CValue<CXSourceLocation>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = kni_clang_getCursor(_arg0, _arg1, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCursor(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorLocation(arg0: CValue<CXCursor>): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorLocation(_arg0, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getCursorLocation(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorExtent(arg0: CValue<CXCursor>): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorExtent(_arg0, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getCursorExtent(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getCursorType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getCursorType(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTypeSpelling(CT: CValue<CXType>): CValue<CXString> {
    return memScoped {
        val _CT = CT.getPointer(memScope).rawValue
        val res = kni_clang_getTypeSpelling(_CT, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getTypeSpelling(CT: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTypedefDeclUnderlyingType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getTypedefDeclUnderlyingType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getTypedefDeclUnderlyingType(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getEnumDeclIntegerType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getEnumDeclIntegerType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getEnumDeclIntegerType(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getEnumConstantDeclValue(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getEnumConstantDeclValue(_C)
        res
    }
}

private external fun kni_clang_getEnumConstantDeclValue(C: NativePtr): Long

fun clang_getEnumConstantDeclUnsignedValue(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getEnumConstantDeclUnsignedValue(_C)
        res
    }
}

private external fun kni_clang_getEnumConstantDeclUnsignedValue(C: NativePtr): Long

fun clang_getFieldDeclBitWidth(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getFieldDeclBitWidth(_C)
        res
    }
}

private external fun kni_clang_getFieldDeclBitWidth(C: NativePtr): Int

fun clang_Cursor_getNumArguments(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getNumArguments(_C)
        res
    }
}

private external fun kni_clang_Cursor_getNumArguments(C: NativePtr): Int

fun clang_Cursor_getArgument(C: CValue<CXCursor>, i: Int): CValue<CXCursor> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _i = i
        val res = kni_clang_Cursor_getArgument(_C, _i, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getArgument(C: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getNumTemplateArguments(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getNumTemplateArguments(_C)
        res
    }
}

private external fun kni_clang_Cursor_getNumTemplateArguments(C: NativePtr): Int

fun clang_Cursor_getTemplateArgumentKind(C: CValue<CXCursor>, I: Int): CXTemplateArgumentKind {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = kni_clang_Cursor_getTemplateArgumentKind(_C, _I)
        CXTemplateArgumentKind.byValue(res)
    }
}

private external fun kni_clang_Cursor_getTemplateArgumentKind(C: NativePtr, I: Int): Int

fun clang_Cursor_getTemplateArgumentType(C: CValue<CXCursor>, I: Int): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = kni_clang_Cursor_getTemplateArgumentType(_C, _I, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getTemplateArgumentType(C: NativePtr, I: Int, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getTemplateArgumentValue(C: CValue<CXCursor>, I: Int): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = kni_clang_Cursor_getTemplateArgumentValue(_C, _I)
        res
    }
}

private external fun kni_clang_Cursor_getTemplateArgumentValue(C: NativePtr, I: Int): Long

fun clang_Cursor_getTemplateArgumentUnsignedValue(C: CValue<CXCursor>, I: Int): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _I = I
        val res = kni_clang_Cursor_getTemplateArgumentUnsignedValue(_C, _I)
        res
    }
}

private external fun kni_clang_Cursor_getTemplateArgumentUnsignedValue(C: NativePtr, I: Int): Long

fun clang_equalTypes(A: CValue<CXType>, B: CValue<CXType>): Int {
    return memScoped {
        val _A = A.getPointer(memScope).rawValue
        val _B = B.getPointer(memScope).rawValue
        val res = kni_clang_equalTypes(_A, _B)
        res
    }
}

private external fun kni_clang_equalTypes(A: NativePtr, B: NativePtr): Int

fun clang_getCanonicalType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getCanonicalType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getCanonicalType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_isConstQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_isConstQualifiedType(_T)
        res
    }
}

private external fun kni_clang_isConstQualifiedType(T: NativePtr): Int

fun clang_Cursor_isMacroFunctionLike(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isMacroFunctionLike(_C)
        res
    }
}

private external fun kni_clang_Cursor_isMacroFunctionLike(C: NativePtr): Int

fun clang_Cursor_isMacroBuiltin(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isMacroBuiltin(_C)
        res
    }
}

private external fun kni_clang_Cursor_isMacroBuiltin(C: NativePtr): Int

fun clang_Cursor_isFunctionInlined(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isFunctionInlined(_C)
        res
    }
}

private external fun kni_clang_Cursor_isFunctionInlined(C: NativePtr): Int

fun clang_isVolatileQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_isVolatileQualifiedType(_T)
        res
    }
}

private external fun kni_clang_isVolatileQualifiedType(T: NativePtr): Int

fun clang_isRestrictQualifiedType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_isRestrictQualifiedType(_T)
        res
    }
}

private external fun kni_clang_isRestrictQualifiedType(T: NativePtr): Int

fun clang_getPointeeType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getPointeeType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getPointeeType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTypeDeclaration(T: CValue<CXType>): CValue<CXCursor> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getTypeDeclaration(_T, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getTypeDeclaration(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getDeclObjCTypeEncoding(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getDeclObjCTypeEncoding(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getDeclObjCTypeEncoding(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Type_getObjCEncoding(type: CValue<CXType>): CValue<CXString> {
    return memScoped {
        val _type = type.getPointer(memScope).rawValue
        val res = kni_clang_Type_getObjCEncoding(_type, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Type_getObjCEncoding(type: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTypeKindSpelling(K: CXTypeKind): CValue<CXString> {
    return memScoped {
        val _K = K.value
        val res = kni_clang_getTypeKindSpelling(_K, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getTypeKindSpelling(K: Int, retValPlacement: NativePtr): NativePtr

fun clang_getFunctionTypeCallingConv(T: CValue<CXType>): CXCallingConv {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getFunctionTypeCallingConv(_T)
        CXCallingConv.byValue(res)
    }
}

private external fun kni_clang_getFunctionTypeCallingConv(T: NativePtr): Int

fun clang_getResultType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getResultType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getResultType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getNumArgTypes(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getNumArgTypes(_T)
        res
    }
}

private external fun kni_clang_getNumArgTypes(T: NativePtr): Int

fun clang_getArgType(T: CValue<CXType>, i: Int): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _i = i
        val res = kni_clang_getArgType(_T, _i, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getArgType(T: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr

fun clang_isFunctionTypeVariadic(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_isFunctionTypeVariadic(_T)
        res
    }
}

private external fun kni_clang_isFunctionTypeVariadic(T: NativePtr): Int

fun clang_getCursorResultType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getCursorResultType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getCursorResultType(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_isPODType(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_isPODType(_T)
        res
    }
}

private external fun kni_clang_isPODType(T: NativePtr): Int

fun clang_getElementType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getElementType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getElementType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getNumElements(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getNumElements(_T)
        res
    }
}

private external fun kni_clang_getNumElements(T: NativePtr): Long

fun clang_getArrayElementType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getArrayElementType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getArrayElementType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getArraySize(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_getArraySize(_T)
        res
    }
}

private external fun kni_clang_getArraySize(T: NativePtr): Long

fun clang_Type_getNamedType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getNamedType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_Type_getNamedType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Type_getAlignOf(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getAlignOf(_T)
        res
    }
}

private external fun kni_clang_Type_getAlignOf(T: NativePtr): Long

fun clang_Type_getClassType(T: CValue<CXType>): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getClassType(_T, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_Type_getClassType(T: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Type_getSizeOf(T: CValue<CXType>): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getSizeOf(_T)
        res
    }
}

private external fun kni_clang_Type_getSizeOf(T: NativePtr): Long

fun clang_Type_getOffsetOf(T: CValue<CXType>, S: String?): Long {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _S = S?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_Type_getOffsetOf(_T, _S)
        res
    }
}

private external fun kni_clang_Type_getOffsetOf(T: NativePtr, S: NativePtr): Long

fun clang_Cursor_getOffsetOfField(C: CValue<CXCursor>): Long {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getOffsetOfField(_C)
        res
    }
}

private external fun kni_clang_Cursor_getOffsetOfField(C: NativePtr): Long

fun clang_Cursor_isAnonymous(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isAnonymous(_C)
        res
    }
}

private external fun kni_clang_Cursor_isAnonymous(C: NativePtr): Int

fun clang_Type_getNumTemplateArguments(T: CValue<CXType>): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getNumTemplateArguments(_T)
        res
    }
}

private external fun kni_clang_Type_getNumTemplateArguments(T: NativePtr): Int

fun clang_Type_getTemplateArgumentAsType(T: CValue<CXType>, i: Int): CValue<CXType> {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _i = i
        val res = kni_clang_Type_getTemplateArgumentAsType(_T, _i, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_Type_getTemplateArgumentAsType(T: NativePtr, i: Int, retValPlacement: NativePtr): NativePtr

fun clang_Type_getCXXRefQualifier(T: CValue<CXType>): CXRefQualifierKind {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val res = kni_clang_Type_getCXXRefQualifier(_T)
        res
    }
}

private external fun kni_clang_Type_getCXXRefQualifier(T: NativePtr): Int

fun clang_Cursor_isBitField(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isBitField(_C)
        res
    }
}

private external fun kni_clang_Cursor_isBitField(C: NativePtr): Int

fun clang_isVirtualBase(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_isVirtualBase(_arg0)
        res
    }
}

private external fun kni_clang_isVirtualBase(arg0: NativePtr): Int

fun clang_getCXXAccessSpecifier(arg0: CValue<CXCursor>): CX_CXXAccessSpecifier {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCXXAccessSpecifier(_arg0)
        CX_CXXAccessSpecifier.byValue(res)
    }
}

private external fun kni_clang_getCXXAccessSpecifier(arg0: NativePtr): Int

fun clang_Cursor_getStorageClass(arg0: CValue<CXCursor>): CX_StorageClass {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getStorageClass(_arg0)
        CX_StorageClass.byValue(res)
    }
}

private external fun kni_clang_Cursor_getStorageClass(arg0: NativePtr): Int

fun clang_getNumOverloadedDecls(cursor: CValue<CXCursor>): Int {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getNumOverloadedDecls(_cursor)
        res
    }
}

private external fun kni_clang_getNumOverloadedDecls(cursor: NativePtr): Int

fun clang_getOverloadedDecl(cursor: CValue<CXCursor>, index: Int): CValue<CXCursor> {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _index = index
        val res = kni_clang_getOverloadedDecl(_cursor, _index, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getOverloadedDecl(cursor: NativePtr, index: Int, retValPlacement: NativePtr): NativePtr

fun clang_getIBOutletCollectionType(arg0: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getIBOutletCollectionType(_arg0, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_getIBOutletCollectionType(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_visitChildren(parent: CValue<CXCursor>, visitor: CXCursorVisitor?, client_data: CXClientData?): Int {
    return memScoped {
        val _parent = parent.getPointer(memScope).rawValue
        val _visitor = visitor.rawValue
        val _client_data = client_data.rawValue
        val res = kni_clang_visitChildren(_parent, _visitor, _client_data)
        res
    }
}

private external fun kni_clang_visitChildren(parent: NativePtr, visitor: NativePtr, client_data: NativePtr): Int

fun clang_getCursorUSR(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorUSR(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCursorUSR(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCClass(class_name: String?): CValue<CXString> {
    return memScoped {
        val _class_name = class_name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCClass(_class_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCClass(class_name: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCCategory(class_name: String?, category_name: String?): CValue<CXString> {
    return memScoped {
        val _class_name = class_name?.cstr?.getPointer(memScope).rawValue
        val _category_name = category_name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCCategory(_class_name, _category_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCCategory(class_name: NativePtr, category_name: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCProtocol(protocol_name: String?): CValue<CXString> {
    return memScoped {
        val _protocol_name = protocol_name?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCProtocol(_protocol_name, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCProtocol(protocol_name: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCIvar(name: String?, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCIvar(_name, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCIvar(name: NativePtr, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCMethod(name: String?, isInstanceMethod: Int, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _name = name?.cstr?.getPointer(memScope).rawValue
        val _isInstanceMethod = isInstanceMethod
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCMethod(_name, _isInstanceMethod, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCMethod(name: NativePtr, isInstanceMethod: Int, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_constructUSR_ObjCProperty(property: String?, classUSR: CValue<CXString>): CValue<CXString> {
    return memScoped {
        val _property = property?.cstr?.getPointer(memScope).rawValue
        val _classUSR = classUSR.getPointer(memScope).rawValue
        val res = kni_clang_constructUSR_ObjCProperty(_property, _classUSR, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_constructUSR_ObjCProperty(property: NativePtr, classUSR: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorSpelling(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorSpelling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCursorSpelling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getSpellingNameRange(arg0: CValue<CXCursor>, pieceIndex: Int, options: Int): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _pieceIndex = pieceIndex
        val _options = options
        val res = kni_clang_Cursor_getSpellingNameRange(_arg0, _pieceIndex, _options, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getSpellingNameRange(arg0: NativePtr, pieceIndex: Int, options: Int, retValPlacement: NativePtr): NativePtr

fun clang_getCursorDisplayName(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorDisplayName(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCursorDisplayName(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorReferenced(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorReferenced(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCursorReferenced(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorDefinition(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCursorDefinition(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCursorDefinition(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_isCursorDefinition(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_isCursorDefinition(_arg0)
        res
    }
}

private external fun kni_clang_isCursorDefinition(arg0: NativePtr): Int

fun clang_getCanonicalCursor(arg0: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getCanonicalCursor(_arg0, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getCanonicalCursor(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getObjCSelectorIndex(arg0: CValue<CXCursor>): Int {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getObjCSelectorIndex(_arg0)
        res
    }
}

private external fun kni_clang_Cursor_getObjCSelectorIndex(arg0: NativePtr): Int

fun clang_Cursor_isDynamicCall(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isDynamicCall(_C)
        res
    }
}

private external fun kni_clang_Cursor_isDynamicCall(C: NativePtr): Int

fun clang_Cursor_getReceiverType(C: CValue<CXCursor>): CValue<CXType> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getReceiverType(_C, alloc<CXType>().rawPtr)
        interpretPointed<CXType>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getReceiverType(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getObjCPropertyAttributes(C: CValue<CXCursor>, reserved: Int): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _reserved = reserved
        val res = kni_clang_Cursor_getObjCPropertyAttributes(_C, _reserved)
        res
    }
}

private external fun kni_clang_Cursor_getObjCPropertyAttributes(C: NativePtr, reserved: Int): Int

fun clang_Cursor_getObjCDeclQualifiers(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getObjCDeclQualifiers(_C)
        res
    }
}

private external fun kni_clang_Cursor_getObjCDeclQualifiers(C: NativePtr): Int

fun clang_Cursor_isObjCOptional(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isObjCOptional(_C)
        res
    }
}

private external fun kni_clang_Cursor_isObjCOptional(C: NativePtr): Int

fun clang_Cursor_isVariadic(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_isVariadic(_C)
        res
    }
}

private external fun kni_clang_Cursor_isVariadic(C: NativePtr): Int

fun clang_Cursor_getCommentRange(C: CValue<CXCursor>): CValue<CXSourceRange> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getCommentRange(_C, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getCommentRange(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getRawCommentText(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getRawCommentText(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getRawCommentText(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getBriefCommentText(C: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getBriefCommentText(_C, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getBriefCommentText(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getMangling(arg0: CValue<CXCursor>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getMangling(_arg0, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Cursor_getMangling(arg0: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Cursor_getCXXManglings(arg0: CValue<CXCursor>): CPointer<CXStringSet>? {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getCXXManglings(_arg0)
        interpretCPointer<CXStringSet>(res)
    }
}

private external fun kni_clang_Cursor_getCXXManglings(arg0: NativePtr): NativePtr

fun clang_Cursor_getModule(C: CValue<CXCursor>): CXModule? {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_getModule(_C)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_Cursor_getModule(C: NativePtr): NativePtr

fun clang_getModuleForFile(arg0: CXTranslationUnit?, arg1: CXFile?): CXModule? {
    val _arg0 = arg0.rawValue
    val _arg1 = arg1.rawValue
    val res = kni_clang_getModuleForFile(_arg0, _arg1)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getModuleForFile(arg0: NativePtr, arg1: NativePtr): NativePtr

fun clang_Module_getASTFile(Module: CXModule?): CXFile? {
    val _Module = Module.rawValue
    val res = kni_clang_Module_getASTFile(_Module)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_Module_getASTFile(Module: NativePtr): NativePtr

fun clang_Module_getParent(Module: CXModule?): CXModule? {
    val _Module = Module.rawValue
    val res = kni_clang_Module_getParent(_Module)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_Module_getParent(Module: NativePtr): NativePtr

fun clang_Module_getName(Module: CXModule?): CValue<CXString> {
    return memScoped {
        val _Module = Module.rawValue
        val res = kni_clang_Module_getName(_Module, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Module_getName(Module: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Module_getFullName(Module: CXModule?): CValue<CXString> {
    return memScoped {
        val _Module = Module.rawValue
        val res = kni_clang_Module_getFullName(_Module, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_Module_getFullName(Module: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Module_isSystem(Module: CXModule?): Int {
    val _Module = Module.rawValue
    val res = kni_clang_Module_isSystem(_Module)
    return res
}

private external fun kni_clang_Module_isSystem(Module: NativePtr): Int

fun clang_Module_getNumTopLevelHeaders(arg0: CXTranslationUnit?, Module: CXModule?): Int {
    val _arg0 = arg0.rawValue
    val _Module = Module.rawValue
    val res = kni_clang_Module_getNumTopLevelHeaders(_arg0, _Module)
    return res
}

private external fun kni_clang_Module_getNumTopLevelHeaders(arg0: NativePtr, Module: NativePtr): Int

fun clang_Module_getTopLevelHeader(arg0: CXTranslationUnit?, Module: CXModule?, Index: Int): CXFile? {
    val _arg0 = arg0.rawValue
    val _Module = Module.rawValue
    val _Index = Index
    val res = kni_clang_Module_getTopLevelHeader(_arg0, _Module, _Index)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_Module_getTopLevelHeader(arg0: NativePtr, Module: NativePtr, Index: Int): NativePtr

fun clang_CXXConstructor_isConvertingConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXConstructor_isConvertingConstructor(_C)
        res
    }
}

private external fun kni_clang_CXXConstructor_isConvertingConstructor(C: NativePtr): Int

fun clang_CXXConstructor_isCopyConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXConstructor_isCopyConstructor(_C)
        res
    }
}

private external fun kni_clang_CXXConstructor_isCopyConstructor(C: NativePtr): Int

fun clang_CXXConstructor_isDefaultConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXConstructor_isDefaultConstructor(_C)
        res
    }
}

private external fun kni_clang_CXXConstructor_isDefaultConstructor(C: NativePtr): Int

fun clang_CXXConstructor_isMoveConstructor(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXConstructor_isMoveConstructor(_C)
        res
    }
}

private external fun kni_clang_CXXConstructor_isMoveConstructor(C: NativePtr): Int

fun clang_CXXField_isMutable(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXField_isMutable(_C)
        res
    }
}

private external fun kni_clang_CXXField_isMutable(C: NativePtr): Int

fun clang_CXXMethod_isDefaulted(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXMethod_isDefaulted(_C)
        res
    }
}

private external fun kni_clang_CXXMethod_isDefaulted(C: NativePtr): Int

fun clang_CXXMethod_isPureVirtual(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXMethod_isPureVirtual(_C)
        res
    }
}

private external fun kni_clang_CXXMethod_isPureVirtual(C: NativePtr): Int

fun clang_CXXMethod_isStatic(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXMethod_isStatic(_C)
        res
    }
}

private external fun kni_clang_CXXMethod_isStatic(C: NativePtr): Int

fun clang_CXXMethod_isVirtual(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXMethod_isVirtual(_C)
        res
    }
}

private external fun kni_clang_CXXMethod_isVirtual(C: NativePtr): Int

fun clang_CXXMethod_isConst(C: CValue<CXCursor>): Int {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_CXXMethod_isConst(_C)
        res
    }
}

private external fun kni_clang_CXXMethod_isConst(C: NativePtr): Int

fun clang_getTemplateCursorKind(C: CValue<CXCursor>): CXCursorKind {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getTemplateCursorKind(_C)
        CXCursorKind.byValue(res)
    }
}

private external fun kni_clang_getTemplateCursorKind(C: NativePtr): Int

fun clang_getSpecializedCursorTemplate(C: CValue<CXCursor>): CValue<CXCursor> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_getSpecializedCursorTemplate(_C, alloc<CXCursor>().rawPtr)
        interpretPointed<CXCursor>(res).readValue()
    }
}

private external fun kni_clang_getSpecializedCursorTemplate(C: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorReferenceNameRange(C: CValue<CXCursor>, NameFlags: Int, PieceIndex: Int): CValue<CXSourceRange> {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val _NameFlags = NameFlags
        val _PieceIndex = PieceIndex
        val res = kni_clang_getCursorReferenceNameRange(_C, _NameFlags, _PieceIndex, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getCursorReferenceNameRange(C: NativePtr, NameFlags: Int, PieceIndex: Int, retValPlacement: NativePtr): NativePtr

fun clang_getTokenKind(arg0: CValue<CXToken>): CXTokenKind {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val res = kni_clang_getTokenKind(_arg0)
        CXTokenKind.byValue(res)
    }
}

private external fun kni_clang_getTokenKind(arg0: NativePtr): Int

fun clang_getTokenSpelling(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXString> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = kni_clang_getTokenSpelling(_arg0, _arg1, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getTokenSpelling(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTokenLocation(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceLocation> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = kni_clang_getTokenLocation(_arg0, _arg1, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_getTokenLocation(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getTokenExtent(arg0: CXTranslationUnit?, arg1: CValue<CXToken>): CValue<CXSourceRange> {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _arg1 = arg1.getPointer(memScope).rawValue
        val res = kni_clang_getTokenExtent(_arg0, _arg1, alloc<CXSourceRange>().rawPtr)
        interpretPointed<CXSourceRange>(res).readValue()
    }
}

private external fun kni_clang_getTokenExtent(arg0: NativePtr, arg1: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_tokenize(TU: CXTranslationUnit?, Range: CValue<CXSourceRange>, Tokens: CValuesRef<CPointerVar<CXToken>>?, NumTokens: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Range = Range.getPointer(memScope).rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens?.getPointer(memScope).rawValue
        val res = kni_clang_tokenize(_TU, _Range, _Tokens, _NumTokens)
        res
    }
}

private external fun kni_clang_tokenize(TU: NativePtr, Range: NativePtr, Tokens: NativePtr, NumTokens: NativePtr): Unit

fun clang_annotateTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int, Cursors: CValuesRef<CXCursor>?): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens
        val _Cursors = Cursors?.getPointer(memScope).rawValue
        val res = kni_clang_annotateTokens(_TU, _Tokens, _NumTokens, _Cursors)
        res
    }
}

private external fun kni_clang_annotateTokens(TU: NativePtr, Tokens: NativePtr, NumTokens: Int, Cursors: NativePtr): Unit

fun clang_disposeTokens(TU: CXTranslationUnit?, Tokens: CValuesRef<CXToken>?, NumTokens: Int): Unit {
    return memScoped {
        val _TU = TU.rawValue
        val _Tokens = Tokens?.getPointer(memScope).rawValue
        val _NumTokens = NumTokens
        val res = kni_clang_disposeTokens(_TU, _Tokens, _NumTokens)
        res
    }
}

private external fun kni_clang_disposeTokens(TU: NativePtr, Tokens: NativePtr, NumTokens: Int): Unit

fun clang_getCursorKindSpelling(Kind: CXCursorKind): CValue<CXString> {
    return memScoped {
        val _Kind = Kind.value
        val res = kni_clang_getCursorKindSpelling(_Kind, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCursorKindSpelling(Kind: Int, retValPlacement: NativePtr): NativePtr

fun clang_getDefinitionSpellingAndExtent(arg0: CValue<CXCursor>, startBuf: CValuesRef<CPointerVar<ByteVar>>?, endBuf: CValuesRef<CPointerVar<ByteVar>>?, startLine: CValuesRef<IntVar>?, startColumn: CValuesRef<IntVar>?, endLine: CValuesRef<IntVar>?, endColumn: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _arg0 = arg0.getPointer(memScope).rawValue
        val _startBuf = startBuf?.getPointer(memScope).rawValue
        val _endBuf = endBuf?.getPointer(memScope).rawValue
        val _startLine = startLine?.getPointer(memScope).rawValue
        val _startColumn = startColumn?.getPointer(memScope).rawValue
        val _endLine = endLine?.getPointer(memScope).rawValue
        val _endColumn = endColumn?.getPointer(memScope).rawValue
        val res = kni_clang_getDefinitionSpellingAndExtent(_arg0, _startBuf, _endBuf, _startLine, _startColumn, _endLine, _endColumn)
        res
    }
}

private external fun kni_clang_getDefinitionSpellingAndExtent(arg0: NativePtr, startBuf: NativePtr, endBuf: NativePtr, startLine: NativePtr, startColumn: NativePtr, endLine: NativePtr, endColumn: NativePtr): Unit

fun clang_enableStackTraces(): Unit {
    val res = kni_clang_enableStackTraces()
    return res
}

private external fun kni_clang_enableStackTraces(): Unit

fun clang_executeOnThread(fn: CPointer<CFunction<(COpaquePointer?) -> Unit>>?, user_data: COpaquePointer?, stack_size: Int): Unit {
    val _fn = fn.rawValue
    val _user_data = user_data.rawValue
    val _stack_size = stack_size
    val res = kni_clang_executeOnThread(_fn, _user_data, _stack_size)
    return res
}

private external fun kni_clang_executeOnThread(fn: NativePtr, user_data: NativePtr, stack_size: Int): Unit

fun clang_getCompletionChunkKind(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionChunkKind {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val res = kni_clang_getCompletionChunkKind(_completion_string, _chunk_number)
    return CXCompletionChunkKind.byValue(res)
}

private external fun kni_clang_getCompletionChunkKind(completion_string: NativePtr, chunk_number: Int): Int

fun clang_getCompletionChunkText(completion_string: CXCompletionString?, chunk_number: Int): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _chunk_number = chunk_number
        val res = kni_clang_getCompletionChunkText(_completion_string, _chunk_number, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCompletionChunkText(completion_string: NativePtr, chunk_number: Int, retValPlacement: NativePtr): NativePtr

fun clang_getCompletionChunkCompletionString(completion_string: CXCompletionString?, chunk_number: Int): CXCompletionString? {
    val _completion_string = completion_string.rawValue
    val _chunk_number = chunk_number
    val res = kni_clang_getCompletionChunkCompletionString(_completion_string, _chunk_number)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_getCompletionChunkCompletionString(completion_string: NativePtr, chunk_number: Int): NativePtr

fun clang_getNumCompletionChunks(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = kni_clang_getNumCompletionChunks(_completion_string)
    return res
}

private external fun kni_clang_getNumCompletionChunks(completion_string: NativePtr): Int

fun clang_getCompletionPriority(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = kni_clang_getCompletionPriority(_completion_string)
    return res
}

private external fun kni_clang_getCompletionPriority(completion_string: NativePtr): Int

fun clang_getCompletionAvailability(completion_string: CXCompletionString?): CXAvailabilityKind {
    val _completion_string = completion_string.rawValue
    val res = kni_clang_getCompletionAvailability(_completion_string)
    return CXAvailabilityKind.byValue(res)
}

private external fun kni_clang_getCompletionAvailability(completion_string: NativePtr): Int

fun clang_getCompletionNumAnnotations(completion_string: CXCompletionString?): Int {
    val _completion_string = completion_string.rawValue
    val res = kni_clang_getCompletionNumAnnotations(_completion_string)
    return res
}

private external fun kni_clang_getCompletionNumAnnotations(completion_string: NativePtr): Int

fun clang_getCompletionAnnotation(completion_string: CXCompletionString?, annotation_number: Int): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _annotation_number = annotation_number
        val res = kni_clang_getCompletionAnnotation(_completion_string, _annotation_number, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCompletionAnnotation(completion_string: NativePtr, annotation_number: Int, retValPlacement: NativePtr): NativePtr

fun clang_getCompletionParent(completion_string: CXCompletionString?, kind: CValuesRef<CXCursorKind.Var>?): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val _kind = kind?.getPointer(memScope).rawValue
        val res = kni_clang_getCompletionParent(_completion_string, _kind, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCompletionParent(completion_string: NativePtr, kind: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCompletionBriefComment(completion_string: CXCompletionString?): CValue<CXString> {
    return memScoped {
        val _completion_string = completion_string.rawValue
        val res = kni_clang_getCompletionBriefComment(_completion_string, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getCompletionBriefComment(completion_string: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getCursorCompletionString(cursor: CValue<CXCursor>): CXCompletionString? {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val res = kni_clang_getCursorCompletionString(_cursor)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_getCursorCompletionString(cursor: NativePtr): NativePtr

fun clang_defaultCodeCompleteOptions(): Int {
    val res = kni_clang_defaultCodeCompleteOptions()
    return res
}

private external fun kni_clang_defaultCodeCompleteOptions(): Int

fun clang_codeCompleteAt(TU: CXTranslationUnit?, complete_filename: String?, complete_line: Int, complete_column: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, options: Int): CPointer<CXCodeCompleteResults>? {
    return memScoped {
        val _TU = TU.rawValue
        val _complete_filename = complete_filename?.cstr?.getPointer(memScope).rawValue
        val _complete_line = complete_line
        val _complete_column = complete_column
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _options = options
        val res = kni_clang_codeCompleteAt(_TU, _complete_filename, _complete_line, _complete_column, _unsaved_files, _num_unsaved_files, _options)
        interpretCPointer<CXCodeCompleteResults>(res)
    }
}

private external fun kni_clang_codeCompleteAt(TU: NativePtr, complete_filename: NativePtr, complete_line: Int, complete_column: Int, unsaved_files: NativePtr, num_unsaved_files: Int, options: Int): NativePtr

fun clang_sortCodeCompletionResults(Results: CValuesRef<CXCompletionResult>?, NumResults: Int): Unit {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _NumResults = NumResults
        val res = kni_clang_sortCodeCompletionResults(_Results, _NumResults)
        res
    }
}

private external fun kni_clang_sortCodeCompletionResults(Results: NativePtr, NumResults: Int): Unit

fun clang_disposeCodeCompleteResults(Results: CValuesRef<CXCodeCompleteResults>?): Unit {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = kni_clang_disposeCodeCompleteResults(_Results)
        res
    }
}

private external fun kni_clang_disposeCodeCompleteResults(Results: NativePtr): Unit

fun clang_codeCompleteGetNumDiagnostics(Results: CValuesRef<CXCodeCompleteResults>?): Int {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = kni_clang_codeCompleteGetNumDiagnostics(_Results)
        res
    }
}

private external fun kni_clang_codeCompleteGetNumDiagnostics(Results: NativePtr): Int

fun clang_codeCompleteGetDiagnostic(Results: CValuesRef<CXCodeCompleteResults>?, Index: Int): CXDiagnostic? {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _Index = Index
        val res = kni_clang_codeCompleteGetDiagnostic(_Results, _Index)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_codeCompleteGetDiagnostic(Results: NativePtr, Index: Int): NativePtr

fun clang_codeCompleteGetContexts(Results: CValuesRef<CXCodeCompleteResults>?): Long {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = kni_clang_codeCompleteGetContexts(_Results)
        res
    }
}

private external fun kni_clang_codeCompleteGetContexts(Results: NativePtr): Long

fun clang_codeCompleteGetContainerKind(Results: CValuesRef<CXCodeCompleteResults>?, IsIncomplete: CValuesRef<IntVar>?): CXCursorKind {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val _IsIncomplete = IsIncomplete?.getPointer(memScope).rawValue
        val res = kni_clang_codeCompleteGetContainerKind(_Results, _IsIncomplete)
        CXCursorKind.byValue(res)
    }
}

private external fun kni_clang_codeCompleteGetContainerKind(Results: NativePtr, IsIncomplete: NativePtr): Int

fun clang_codeCompleteGetContainerUSR(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = kni_clang_codeCompleteGetContainerUSR(_Results, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_codeCompleteGetContainerUSR(Results: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_codeCompleteGetObjCSelector(Results: CValuesRef<CXCodeCompleteResults>?): CValue<CXString> {
    return memScoped {
        val _Results = Results?.getPointer(memScope).rawValue
        val res = kni_clang_codeCompleteGetObjCSelector(_Results, alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_codeCompleteGetObjCSelector(Results: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_getClangVersion(): CValue<CXString> {
    return memScoped {
        val res = kni_clang_getClangVersion(alloc<CXString>().rawPtr)
        interpretPointed<CXString>(res).readValue()
    }
}

private external fun kni_clang_getClangVersion(retValPlacement: NativePtr): NativePtr

fun clang_toggleCrashRecovery(isEnabled: Int): Unit {
    val _isEnabled = isEnabled
    val res = kni_clang_toggleCrashRecovery(_isEnabled)
    return res
}

private external fun kni_clang_toggleCrashRecovery(isEnabled: Int): Unit

fun clang_getInclusions(tu: CXTranslationUnit?, visitor: CXInclusionVisitor?, client_data: CXClientData?): Unit {
    val _tu = tu.rawValue
    val _visitor = visitor.rawValue
    val _client_data = client_data.rawValue
    val res = kni_clang_getInclusions(_tu, _visitor, _client_data)
    return res
}

private external fun kni_clang_getInclusions(tu: NativePtr, visitor: NativePtr, client_data: NativePtr): Unit

fun clang_Cursor_Evaluate(C: CValue<CXCursor>): CXEvalResult? {
    return memScoped {
        val _C = C.getPointer(memScope).rawValue
        val res = kni_clang_Cursor_Evaluate(_C)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_Cursor_Evaluate(C: NativePtr): NativePtr

fun clang_EvalResult_getKind(E: CXEvalResult?): CXEvalResultKind {
    val _E = E.rawValue
    val res = kni_clang_EvalResult_getKind(_E)
    return CXEvalResultKind.byValue(res)
}

private external fun kni_clang_EvalResult_getKind(E: NativePtr): Int

fun clang_EvalResult_getAsInt(E: CXEvalResult?): Int {
    val _E = E.rawValue
    val res = kni_clang_EvalResult_getAsInt(_E)
    return res
}

private external fun kni_clang_EvalResult_getAsInt(E: NativePtr): Int

fun clang_EvalResult_getAsDouble(E: CXEvalResult?): Double {
    val _E = E.rawValue
    val res = kni_clang_EvalResult_getAsDouble(_E)
    return res
}

private external fun kni_clang_EvalResult_getAsDouble(E: NativePtr): Double

fun clang_EvalResult_getAsStr(E: CXEvalResult?): CPointer<ByteVar>? {
    val _E = E.rawValue
    val res = kni_clang_EvalResult_getAsStr(_E)
    return interpretCPointer<ByteVar>(res)
}

private external fun kni_clang_EvalResult_getAsStr(E: NativePtr): NativePtr

fun clang_EvalResult_dispose(E: CXEvalResult?): Unit {
    val _E = E.rawValue
    val res = kni_clang_EvalResult_dispose(_E)
    return res
}

private external fun kni_clang_EvalResult_dispose(E: NativePtr): Unit

fun clang_getRemappings(path: String?): CXRemapping? {
    return memScoped {
        val _path = path?.cstr?.getPointer(memScope).rawValue
        val res = kni_clang_getRemappings(_path)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_getRemappings(path: NativePtr): NativePtr

fun clang_getRemappingsFromFileList(filePaths: CValuesRef<CPointerVar<ByteVar>>?, numFiles: Int): CXRemapping? {
    return memScoped {
        val _filePaths = filePaths?.getPointer(memScope).rawValue
        val _numFiles = numFiles
        val res = kni_clang_getRemappingsFromFileList(_filePaths, _numFiles)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_getRemappingsFromFileList(filePaths: NativePtr, numFiles: Int): NativePtr

fun clang_remap_getNumFiles(arg0: CXRemapping?): Int {
    val _arg0 = arg0.rawValue
    val res = kni_clang_remap_getNumFiles(_arg0)
    return res
}

private external fun kni_clang_remap_getNumFiles(arg0: NativePtr): Int

fun clang_remap_getFilenames(arg0: CXRemapping?, index: Int, original: CValuesRef<CXString>?, transformed: CValuesRef<CXString>?): Unit {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _index = index
        val _original = original?.getPointer(memScope).rawValue
        val _transformed = transformed?.getPointer(memScope).rawValue
        val res = kni_clang_remap_getFilenames(_arg0, _index, _original, _transformed)
        res
    }
}

private external fun kni_clang_remap_getFilenames(arg0: NativePtr, index: Int, original: NativePtr, transformed: NativePtr): Unit

fun clang_remap_dispose(arg0: CXRemapping?): Unit {
    val _arg0 = arg0.rawValue
    val res = kni_clang_remap_dispose(_arg0)
    return res
}

private external fun kni_clang_remap_dispose(arg0: NativePtr): Unit

fun clang_findReferencesInFile(cursor: CValue<CXCursor>, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    return memScoped {
        val _cursor = cursor.getPointer(memScope).rawValue
        val _file = file.rawValue
        val _visitor = visitor.getPointer(memScope).rawValue
        val res = kni_clang_findReferencesInFile(_cursor, _file, _visitor)
        CXResult.byValue(res)
    }
}

private external fun kni_clang_findReferencesInFile(cursor: NativePtr, file: NativePtr, visitor: NativePtr): Int

fun clang_findIncludesInFile(TU: CXTranslationUnit?, file: CXFile?, visitor: CValue<CXCursorAndRangeVisitor>): CXResult {
    return memScoped {
        val _TU = TU.rawValue
        val _file = file.rawValue
        val _visitor = visitor.getPointer(memScope).rawValue
        val res = kni_clang_findIncludesInFile(_TU, _file, _visitor)
        CXResult.byValue(res)
    }
}

private external fun kni_clang_findIncludesInFile(TU: NativePtr, file: NativePtr, visitor: NativePtr): Int

fun clang_index_isEntityObjCContainerKind(arg0: CXIdxEntityKind): Int {
    val _arg0 = arg0.value
    val res = kni_clang_index_isEntityObjCContainerKind(_arg0)
    return res
}

private external fun kni_clang_index_isEntityObjCContainerKind(arg0: Int): Int

fun clang_index_getObjCContainerDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCContainerDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getObjCContainerDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCContainerDeclInfo>(res)
    }
}

private external fun kni_clang_index_getObjCContainerDeclInfo(arg0: NativePtr): NativePtr

fun clang_index_getObjCInterfaceDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCInterfaceDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getObjCInterfaceDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCInterfaceDeclInfo>(res)
    }
}

private external fun kni_clang_index_getObjCInterfaceDeclInfo(arg0: NativePtr): NativePtr

fun clang_index_getObjCCategoryDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCCategoryDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getObjCCategoryDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCCategoryDeclInfo>(res)
    }
}

private external fun kni_clang_index_getObjCCategoryDeclInfo(arg0: NativePtr): NativePtr

fun clang_index_getObjCProtocolRefListInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCProtocolRefListInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getObjCProtocolRefListInfo(_arg0)
        interpretCPointer<CXIdxObjCProtocolRefListInfo>(res)
    }
}

private external fun kni_clang_index_getObjCProtocolRefListInfo(arg0: NativePtr): NativePtr

fun clang_index_getObjCPropertyDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxObjCPropertyDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getObjCPropertyDeclInfo(_arg0)
        interpretCPointer<CXIdxObjCPropertyDeclInfo>(res)
    }
}

private external fun kni_clang_index_getObjCPropertyDeclInfo(arg0: NativePtr): NativePtr

fun clang_index_getIBOutletCollectionAttrInfo(arg0: CValuesRef<CXIdxAttrInfo>?): CPointer<CXIdxIBOutletCollectionAttrInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getIBOutletCollectionAttrInfo(_arg0)
        interpretCPointer<CXIdxIBOutletCollectionAttrInfo>(res)
    }
}

private external fun kni_clang_index_getIBOutletCollectionAttrInfo(arg0: NativePtr): NativePtr

fun clang_index_getCXXClassDeclInfo(arg0: CValuesRef<CXIdxDeclInfo>?): CPointer<CXIdxCXXClassDeclInfo>? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getCXXClassDeclInfo(_arg0)
        interpretCPointer<CXIdxCXXClassDeclInfo>(res)
    }
}

private external fun kni_clang_index_getCXXClassDeclInfo(arg0: NativePtr): NativePtr

fun clang_index_getClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?): CXIdxClientContainer? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getClientContainer(_arg0)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_index_getClientContainer(arg0: NativePtr): NativePtr

fun clang_index_setClientContainer(arg0: CValuesRef<CXIdxContainerInfo>?, arg1: CXIdxClientContainer?): Unit {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1.rawValue
        val res = kni_clang_index_setClientContainer(_arg0, _arg1)
        res
    }
}

private external fun kni_clang_index_setClientContainer(arg0: NativePtr, arg1: NativePtr): Unit

fun clang_index_getClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?): CXIdxClientEntity? {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val res = kni_clang_index_getClientEntity(_arg0)
        interpretCPointer<COpaque>(res)
    }
}

private external fun kni_clang_index_getClientEntity(arg0: NativePtr): NativePtr

fun clang_index_setClientEntity(arg0: CValuesRef<CXIdxEntityInfo>?, arg1: CXIdxClientEntity?): Unit {
    return memScoped {
        val _arg0 = arg0?.getPointer(memScope).rawValue
        val _arg1 = arg1.rawValue
        val res = kni_clang_index_setClientEntity(_arg0, _arg1)
        res
    }
}

private external fun kni_clang_index_setClientEntity(arg0: NativePtr, arg1: NativePtr): Unit

fun clang_IndexAction_create(CIdx: CXIndex?): CXIndexAction? {
    val _CIdx = CIdx.rawValue
    val res = kni_clang_IndexAction_create(_CIdx)
    return interpretCPointer<COpaque>(res)
}

private external fun kni_clang_IndexAction_create(CIdx: NativePtr): NativePtr

fun clang_IndexAction_dispose(arg0: CXIndexAction?): Unit {
    val _arg0 = arg0.rawValue
    val res = kni_clang_IndexAction_dispose(_arg0)
    return res
}

private external fun kni_clang_IndexAction_dispose(arg0: NativePtr): Unit

fun clang_indexSourceFile(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val _TU_options = TU_options
        val res = kni_clang_indexSourceFile(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

private external fun kni_clang_indexSourceFile(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, out_TU: NativePtr, TU_options: Int): Int

fun clang_indexSourceFileFullArgv(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, source_filename: String?, command_line_args: CValuesRef<CPointerVar<ByteVar>>?, num_command_line_args: Int, unsaved_files: CValuesRef<CXUnsavedFile>?, num_unsaved_files: Int, out_TU: CValuesRef<CXTranslationUnitVar>?, TU_options: Int): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _source_filename = source_filename?.cstr?.getPointer(memScope).rawValue
        val _command_line_args = command_line_args?.getPointer(memScope).rawValue
        val _num_command_line_args = num_command_line_args
        val _unsaved_files = unsaved_files?.getPointer(memScope).rawValue
        val _num_unsaved_files = num_unsaved_files
        val _out_TU = out_TU?.getPointer(memScope).rawValue
        val _TU_options = TU_options
        val res = kni_clang_indexSourceFileFullArgv(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _source_filename, _command_line_args, _num_command_line_args, _unsaved_files, _num_unsaved_files, _out_TU, _TU_options)
        res
    }
}

private external fun kni_clang_indexSourceFileFullArgv(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, source_filename: NativePtr, command_line_args: NativePtr, num_command_line_args: Int, unsaved_files: NativePtr, num_unsaved_files: Int, out_TU: NativePtr, TU_options: Int): Int

fun clang_indexTranslationUnit(arg0: CXIndexAction?, client_data: CXClientData?, index_callbacks: CValuesRef<IndexerCallbacks>?, index_callbacks_size: Int, index_options: Int, arg5: CXTranslationUnit?): Int {
    return memScoped {
        val _arg0 = arg0.rawValue
        val _client_data = client_data.rawValue
        val _index_callbacks = index_callbacks?.getPointer(memScope).rawValue
        val _index_callbacks_size = index_callbacks_size
        val _index_options = index_options
        val _arg5 = arg5.rawValue
        val res = kni_clang_indexTranslationUnit(_arg0, _client_data, _index_callbacks, _index_callbacks_size, _index_options, _arg5)
        res
    }
}

private external fun kni_clang_indexTranslationUnit(arg0: NativePtr, client_data: NativePtr, index_callbacks: NativePtr, index_callbacks_size: Int, index_options: Int, arg5: NativePtr): Int

fun clang_indexLoc_getFileLocation(loc: CValue<CXIdxLoc>, indexFile: CValuesRef<CXIdxClientFileVar>?, file: CValuesRef<CXFileVar>?, line: CValuesRef<IntVar>?, column: CValuesRef<IntVar>?, offset: CValuesRef<IntVar>?): Unit {
    return memScoped {
        val _loc = loc.getPointer(memScope).rawValue
        val _indexFile = indexFile?.getPointer(memScope).rawValue
        val _file = file?.getPointer(memScope).rawValue
        val _line = line?.getPointer(memScope).rawValue
        val _column = column?.getPointer(memScope).rawValue
        val _offset = offset?.getPointer(memScope).rawValue
        val res = kni_clang_indexLoc_getFileLocation(_loc, _indexFile, _file, _line, _column, _offset)
        res
    }
}

private external fun kni_clang_indexLoc_getFileLocation(loc: NativePtr, indexFile: NativePtr, file: NativePtr, line: NativePtr, column: NativePtr, offset: NativePtr): Unit

fun clang_indexLoc_getCXSourceLocation(loc: CValue<CXIdxLoc>): CValue<CXSourceLocation> {
    return memScoped {
        val _loc = loc.getPointer(memScope).rawValue
        val res = kni_clang_indexLoc_getCXSourceLocation(_loc, alloc<CXSourceLocation>().rawPtr)
        interpretPointed<CXSourceLocation>(res).readValue()
    }
}

private external fun kni_clang_indexLoc_getCXSourceLocation(loc: NativePtr, retValPlacement: NativePtr): NativePtr

fun clang_Type_visitFields(T: CValue<CXType>, visitor: CXFieldVisitor?, client_data: CXClientData?): Int {
    return memScoped {
        val _T = T.getPointer(memScope).rawValue
        val _visitor = visitor.rawValue
        val _client_data = client_data.rawValue
        val res = kni_clang_Type_visitFields(_T, _visitor, _client_data)
        res
    }
}

private external fun kni_clang_Type_visitFields(T: NativePtr, visitor: NativePtr, client_data: NativePtr): Int

val __llvm__: Int = 1

val __clang__: Int = 1

val __clang_major__: Int = 3

val __clang_minor__: Int = 9

val __clang_patchlevel__: Int = 0

val __GNUC_MINOR__: Int = 2

val __GNUC_PATCHLEVEL__: Int = 1

val __GNUC__: Int = 4

val __GXX_ABI_VERSION: Int = 1002

val __ATOMIC_RELAXED: Int = 0

val __ATOMIC_CONSUME: Int = 1

val __ATOMIC_ACQUIRE: Int = 2

val __ATOMIC_RELEASE: Int = 3

val __ATOMIC_ACQ_REL: Int = 4

val __ATOMIC_SEQ_CST: Int = 5

val __PRAGMA_REDEFINE_EXTNAME: Int = 1

val __STRICT_ANSI__: Int = 1

val __CONSTANT_CFSTRINGS__: Int = 1

val __BLOCKS__: Int = 1

val __ORDER_LITTLE_ENDIAN__: Int = 1234

val __ORDER_BIG_ENDIAN__: Int = 4321

val __ORDER_PDP_ENDIAN__: Int = 3412

val __BYTE_ORDER__: Int = 1234

val __LITTLE_ENDIAN__: Int = 1

val _LP64: Int = 1

val __LP64__: Int = 1

val __CHAR_BIT__: Int = 8

val __SCHAR_MAX__: Int = 127

val __SHRT_MAX__: Int = 32767

val __INT_MAX__: Int = 2147483647

val __LONG_MAX__: Long = 9223372036854775807

val __LONG_LONG_MAX__: Long = 9223372036854775807

val __WCHAR_MAX__: Int = 2147483647

val __INTMAX_MAX__: Long = 9223372036854775807

val __SIZE_MAX__: Long = -1

val __UINTMAX_MAX__: Long = -1

val __PTRDIFF_MAX__: Long = 9223372036854775807

val __INTPTR_MAX__: Long = 9223372036854775807

val __UINTPTR_MAX__: Long = -1

val __SIZEOF_DOUBLE__: Int = 8

val __SIZEOF_FLOAT__: Int = 4

val __SIZEOF_INT__: Int = 4

val __SIZEOF_LONG__: Int = 8

val __SIZEOF_LONG_DOUBLE__: Int = 16

val __SIZEOF_LONG_LONG__: Int = 8

val __SIZEOF_POINTER__: Int = 8

val __SIZEOF_SHORT__: Int = 2

val __SIZEOF_PTRDIFF_T__: Int = 8

val __SIZEOF_SIZE_T__: Int = 8

val __SIZEOF_WCHAR_T__: Int = 4

val __SIZEOF_WINT_T__: Int = 4

val __SIZEOF_INT128__: Int = 16

val __INTMAX_WIDTH__: Int = 64

val __PTRDIFF_WIDTH__: Int = 64

val __INTPTR_WIDTH__: Int = 64

val __SIZE_WIDTH__: Int = 64

val __WCHAR_WIDTH__: Int = 32

val __WINT_WIDTH__: Int = 32

val __SIG_ATOMIC_WIDTH__: Int = 32

val __SIG_ATOMIC_MAX__: Int = 2147483647

val __UINTMAX_WIDTH__: Int = 64

val __UINTPTR_WIDTH__: Int = 64

val __FLT_DENORM_MIN__: Float = bitsToFloat(1) /* == 1.4E-45 */

val __FLT_HAS_DENORM__: Int = 1

val __FLT_DIG__: Int = 6

val __FLT_DECIMAL_DIG__: Int = 9

val __FLT_EPSILON__: Float = bitsToFloat(872415232) /* == 1.1920929E-7 */

val __FLT_HAS_INFINITY__: Int = 1

val __FLT_HAS_QUIET_NAN__: Int = 1

val __FLT_MANT_DIG__: Int = 24

val __FLT_MAX_10_EXP__: Int = 38

val __FLT_MAX_EXP__: Int = 128

val __FLT_MAX__: Float = bitsToFloat(2139095039) /* == 3.4028235E38 */

val __FLT_MIN_10_EXP__: Int = -37

val __FLT_MIN_EXP__: Int = -125

val __FLT_MIN__: Float = bitsToFloat(8388608) /* == 1.17549435E-38 */

val __DBL_DENORM_MIN__: Double = bitsToDouble(1) /* == 4.9E-324 */

val __DBL_HAS_DENORM__: Int = 1

val __DBL_DIG__: Int = 15

val __DBL_DECIMAL_DIG__: Int = 17

val __DBL_EPSILON__: Double = bitsToDouble(4372995238176751616) /* == 2.220446049250313E-16 */

val __DBL_HAS_INFINITY__: Int = 1

val __DBL_HAS_QUIET_NAN__: Int = 1

val __DBL_MANT_DIG__: Int = 53

val __DBL_MAX_10_EXP__: Int = 308

val __DBL_MAX_EXP__: Int = 1024

val __DBL_MAX__: Double = bitsToDouble(9218868437227405311) /* == 1.7976931348623157E308 */

val __DBL_MIN_10_EXP__: Int = -307

val __DBL_MIN_EXP__: Int = -1021

val __DBL_MIN__: Double = bitsToDouble(4503599627370496) /* == 2.2250738585072014E-308 */


val __LDBL_HAS_DENORM__: Int = 1

val __LDBL_DIG__: Int = 18

val __LDBL_DECIMAL_DIG__: Int = 21


val __LDBL_HAS_INFINITY__: Int = 1

val __LDBL_HAS_QUIET_NAN__: Int = 1

val __LDBL_MANT_DIG__: Int = 64

val __LDBL_MAX_10_EXP__: Int = 4932

val __LDBL_MAX_EXP__: Int = 16384


val __LDBL_MIN_10_EXP__: Int = -4931

val __LDBL_MIN_EXP__: Int = -16381


val __POINTER_WIDTH__: Int = 64

val __BIGGEST_ALIGNMENT__: Int = 16

val __UINT8_MAX__: Int = 255

val __INT8_MAX__: Int = 127

val __UINT16_MAX__: Int = 65535

val __INT16_MAX__: Int = 32767

val __UINT32_MAX__: Int = -1

val __INT32_MAX__: Int = 2147483647

val __UINT64_MAX__: Long = -1

val __INT64_MAX__: Long = 9223372036854775807

val __INT_LEAST8_MAX__: Int = 127

val __UINT_LEAST8_MAX__: Int = 255

val __INT_LEAST16_MAX__: Int = 32767

val __UINT_LEAST16_MAX__: Int = 65535

val __INT_LEAST32_MAX__: Int = 2147483647

val __UINT_LEAST32_MAX__: Int = -1

val __INT_LEAST64_MAX__: Long = 9223372036854775807

val __UINT_LEAST64_MAX__: Long = -1

val __INT_FAST8_MAX__: Int = 127

val __UINT_FAST8_MAX__: Int = 255

val __INT_FAST16_MAX__: Int = 32767

val __UINT_FAST16_MAX__: Int = 65535

val __INT_FAST32_MAX__: Int = 2147483647

val __UINT_FAST32_MAX__: Int = -1

val __INT_FAST64_MAX__: Long = 9223372036854775807

val __UINT_FAST64_MAX__: Long = -1

val __FINITE_MATH_ONLY__: Int = 0

val __GNUC_STDC_INLINE__: Int = 1

val __GCC_ATOMIC_TEST_AND_SET_TRUEVAL: Int = 1

val __GCC_ATOMIC_BOOL_LOCK_FREE: Int = 2

val __GCC_ATOMIC_CHAR_LOCK_FREE: Int = 2

val __GCC_ATOMIC_CHAR16_T_LOCK_FREE: Int = 2

val __GCC_ATOMIC_CHAR32_T_LOCK_FREE: Int = 2

val __GCC_ATOMIC_WCHAR_T_LOCK_FREE: Int = 2

val __GCC_ATOMIC_SHORT_LOCK_FREE: Int = 2

val __GCC_ATOMIC_INT_LOCK_FREE: Int = 2

val __GCC_ATOMIC_LONG_LOCK_FREE: Int = 2

val __GCC_ATOMIC_LLONG_LOCK_FREE: Int = 2

val __GCC_ATOMIC_POINTER_LOCK_FREE: Int = 2

val __NO_INLINE__: Int = 1

val __PIC__: Int = 2

val __pic__: Int = 2

val __FLT_EVAL_METHOD__: Int = 0

val __FLT_RADIX__: Int = 2

val __DECIMAL_DIG__: Int = 21

val __SSP__: Int = 1

val __amd64__: Int = 1

val __amd64: Int = 1

val __x86_64: Int = 1

val __x86_64__: Int = 1

val __core2: Int = 1

val __core2__: Int = 1

val __tune_core2__: Int = 1

val __NO_MATH_INLINES: Int = 1

val __FXSR__: Int = 1

val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_16: Int = 1

val __SSSE3__: Int = 1

val __SSE3__: Int = 1

val __SSE2__: Int = 1

val __SSE2_MATH__: Int = 1

val __SSE__: Int = 1

val __SSE_MATH__: Int = 1

val __MMX__: Int = 1

val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_1: Int = 1

val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_2: Int = 1

val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_4: Int = 1

val __GCC_HAVE_SYNC_COMPARE_AND_SWAP_8: Int = 1

val __APPLE_CC__: Int = 6000

val __APPLE__: Int = 1

val OBJC_NEW_PROPERTIES: Int = 1

val __DYNAMIC__: Int = 1

val __ENVIRONMENT_MAC_OS_X_VERSION_MIN_REQUIRED__: Int = 101100

val __MACH__: Int = 1

val __STDC__: Int = 1

val __STDC_HOSTED__: Int = 1

val __STDC_VERSION__: Long = 199901

val __STDC_UTF_16__: Int = 1

val __STDC_UTF_32__: Int = 1

val __DARWIN_ONLY_64_BIT_INO_T: Int = 0

val __DARWIN_ONLY_VERS_1050: Int = 0

val __DARWIN_ONLY_UNIX_CONFORMANCE: Int = 1

val __DARWIN_UNIX03: Int = 1

val __DARWIN_64_BIT_INO_T: Int = 1

val __DARWIN_VERS_1050: Int = 1

val __DARWIN_NON_CANCELABLE: Int = 0

val __DARWIN_C_ANSI: Long = 4096

val __DARWIN_C_FULL: Long = 900000

val __DARWIN_C_LEVEL: Long = 900000

val _DARWIN_FEATURE_64_BIT_INODE: Int = 1

val _DARWIN_FEATURE_ONLY_UNIX_CONFORMANCE: Int = 1

val _DARWIN_FEATURE_UNIX_CONFORMANCE: Int = 3

val __PTHREAD_SIZE__: Int = 8176

val __PTHREAD_ATTR_SIZE__: Int = 56

val __PTHREAD_MUTEXATTR_SIZE__: Int = 8

val __PTHREAD_MUTEX_SIZE__: Int = 56

val __PTHREAD_CONDATTR_SIZE__: Int = 8

val __PTHREAD_COND_SIZE__: Int = 40

val __PTHREAD_ONCE_SIZE__: Int = 8

val __PTHREAD_RWLOCK_SIZE__: Int = 192

val __PTHREAD_RWLOCKATTR_SIZE__: Int = 16

val __DARWIN_WCHAR_MAX: Int = 2147483647

val __DARWIN_WCHAR_MIN: Int = -2147483648

val __DARWIN_WEOF: __darwin_wint_t = -1

val _FORTIFY_SOURCE: Int = 2

val __MAC_10_0: Int = 1000

val __MAC_10_1: Int = 1010

val __MAC_10_2: Int = 1020

val __MAC_10_3: Int = 1030

val __MAC_10_4: Int = 1040

val __MAC_10_5: Int = 1050

val __MAC_10_6: Int = 1060

val __MAC_10_7: Int = 1070

val __MAC_10_8: Int = 1080

val __MAC_10_9: Int = 1090

val __MAC_10_10: Int = 101000

val __MAC_10_10_2: Int = 101002

val __MAC_10_10_3: Int = 101003

val __MAC_10_11: Int = 101100

val __MAC_10_11_2: Int = 101102

val __MAC_10_11_3: Int = 101103

val __MAC_10_11_4: Int = 101104

val __MAC_10_12: Int = 101200

val __IPHONE_2_0: Int = 20000

val __IPHONE_2_1: Int = 20100

val __IPHONE_2_2: Int = 20200

val __IPHONE_3_0: Int = 30000

val __IPHONE_3_1: Int = 30100

val __IPHONE_3_2: Int = 30200

val __IPHONE_4_0: Int = 40000

val __IPHONE_4_1: Int = 40100

val __IPHONE_4_2: Int = 40200

val __IPHONE_4_3: Int = 40300

val __IPHONE_5_0: Int = 50000

val __IPHONE_5_1: Int = 50100

val __IPHONE_6_0: Int = 60000

val __IPHONE_6_1: Int = 60100

val __IPHONE_7_0: Int = 70000

val __IPHONE_7_1: Int = 70100

val __IPHONE_8_0: Int = 80000

val __IPHONE_8_1: Int = 80100

val __IPHONE_8_2: Int = 80200

val __IPHONE_8_3: Int = 80300

val __IPHONE_8_4: Int = 80400

val __IPHONE_9_0: Int = 90000

val __IPHONE_9_1: Int = 90100

val __IPHONE_9_2: Int = 90200

val __IPHONE_9_3: Int = 90300

val __IPHONE_10_0: Int = 100000

val __TVOS_9_0: Int = 90000

val __TVOS_9_1: Int = 90100

val __TVOS_9_2: Int = 90200

val __TVOS_10_0: Int = 100000

val __WATCHOS_1_0: Int = 10000

val __WATCHOS_2_0: Int = 20000

val __WATCHOS_3_0: Int = 30000

val __MAC_OS_X_VERSION_MIN_REQUIRED: Int = 101100

val __MAC_OS_X_VERSION_MAX_ALLOWED: Int = 101200

val CLOCKS_PER_SEC: Int = 1000000

val CLOCK_REALTIME: Int = 0

val CLOCK_MONOTONIC: Int = 6

val CLOCK_MONOTONIC_RAW: Int = 4

val CLOCK_MONOTONIC_RAW_APPROX: Int = 5

val CLOCK_UPTIME_RAW: Int = 8

val CLOCK_UPTIME_RAW_APPROX: Int = 9

val CLOCK_PROCESS_CPUTIME_ID: Int = 12

val CLOCK_THREAD_CPUTIME_ID: Int = 16

val CINDEX_VERSION_MAJOR: Int = 0

val CINDEX_VERSION_MINOR: Int = 35

val CINDEX_VERSION: Int = 35

class __mbstate_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(128, 8)
    
    @CLength(128)
    val __mbstate8: CArrayPointer<ByteVar>
        get() = arrayMemberAt(0)
    
    var _mbstateL: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
}

class __va_list_tag(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var gp_offset: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var fp_offset: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    var overflow_arg_area: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(8).value
        set(value) { memberAt<COpaquePointerVar>(8).value = value }
    
    var reg_save_area: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(16).value
        set(value) { memberAt<COpaquePointerVar>(16).value = value }
    
}

class __builtin_va_list(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
}

@CNaturalStruct("__routine", "__arg", "__next")
class __darwin_pthread_handler_rec(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var __routine: CPointer<CFunction<(COpaquePointer?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(COpaquePointer?) -> Unit>>>(0).value
        set(value) { memberAt<CPointerVar<CFunction<(COpaquePointer?) -> Unit>>>(0).value = value }
    
    var __arg: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(8).value
        set(value) { memberAt<COpaquePointerVar>(8).value = value }
    
    var __next: CPointer<__darwin_pthread_handler_rec>?
        get() = memberAt<CPointerVar<__darwin_pthread_handler_rec>>(16).value
        set(value) { memberAt<CPointerVar<__darwin_pthread_handler_rec>>(16).value = value }
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_attr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(56)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_cond_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(48, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(40)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_condattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(8)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_mutex_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(56)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_mutexattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(8)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_once_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(8)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_rwlock_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(200, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(192)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__opaque")
class _opaque_pthread_rwlockattr_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    @CLength(16)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("__sig", "__cleanup_stack", "__opaque")
class _opaque_pthread_t(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(8192, 8)
    
    var __sig: Long
        get() = memberAt<LongVar>(0).value
        set(value) { memberAt<LongVar>(0).value = value }
    
    var __cleanup_stack: CPointer<__darwin_pthread_handler_rec>?
        get() = memberAt<CPointerVar<__darwin_pthread_handler_rec>>(8).value
        set(value) { memberAt<CPointerVar<__darwin_pthread_handler_rec>>(8).value = value }
    
    @CLength(8176)
    val __opaque: CArrayPointer<ByteVar>
        get() = arrayMemberAt(16)
    
}

@CNaturalStruct("tv_sec", "tv_nsec")
class timespec(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var tv_sec: __darwin_time_t
        get() = memberAt<__darwin_time_tVar>(0).value
        set(value) { memberAt<__darwin_time_tVar>(0).value = value }
    
    var tv_nsec: Long
        get() = memberAt<LongVar>(8).value
        set(value) { memberAt<LongVar>(8).value = value }
    
}

@CNaturalStruct("tm_sec", "tm_min", "tm_hour", "tm_mday", "tm_mon", "tm_year", "tm_wday", "tm_yday", "tm_isdst", "tm_gmtoff", "tm_zone")
class tm(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(56, 8)
    
    var tm_sec: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var tm_min: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    var tm_hour: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
    var tm_mday: Int
        get() = memberAt<IntVar>(12).value
        set(value) { memberAt<IntVar>(12).value = value }
    
    var tm_mon: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
    var tm_year: Int
        get() = memberAt<IntVar>(20).value
        set(value) { memberAt<IntVar>(20).value = value }
    
    var tm_wday: Int
        get() = memberAt<IntVar>(24).value
        set(value) { memberAt<IntVar>(24).value = value }
    
    var tm_yday: Int
        get() = memberAt<IntVar>(28).value
        set(value) { memberAt<IntVar>(28).value = value }
    
    var tm_isdst: Int
        get() = memberAt<IntVar>(32).value
        set(value) { memberAt<IntVar>(32).value = value }
    
    var tm_gmtoff: Long
        get() = memberAt<LongVar>(40).value
        set(value) { memberAt<LongVar>(40).value = value }
    
    var tm_zone: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(48).value
        set(value) { memberAt<CPointerVar<ByteVar>>(48).value = value }
    
}

@CNaturalStruct("data", "private_flags")
class CXString(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var private_flags: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
}

@CNaturalStruct("Strings", "Count")
class CXStringSet(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var Strings: CPointer<CXString>?
        get() = memberAt<CPointerVar<CXString>>(0).value
        set(value) { memberAt<CPointerVar<CXString>>(0).value = value }
    
    var Count: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
}

class CXVirtualFileOverlayImpl(override val rawPtr: NativePtr) : COpaque

class CXModuleMapDescriptorImpl(override val rawPtr: NativePtr) : COpaque

class CXTranslationUnitImpl(override val rawPtr: NativePtr) : COpaque

@CNaturalStruct("Filename", "Contents", "Length")
class CXUnsavedFile(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var Filename: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(0).value
        set(value) { memberAt<CPointerVar<ByteVar>>(0).value = value }
    
    var Contents: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(8).value
        set(value) { memberAt<CPointerVar<ByteVar>>(8).value = value }
    
    var Length: Long
        get() = memberAt<LongVar>(16).value
        set(value) { memberAt<LongVar>(16).value = value }
    
}

@CNaturalStruct("Major", "Minor", "Subminor")
class CXVersion(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(12, 4)
    
    var Major: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var Minor: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    var Subminor: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
}

@CNaturalStruct("data")
class CXFileUniqueID(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    @CLength(3)
    val data: CArrayPointer<LongVar>
        get() = arrayMemberAt(0)
    
}

@CNaturalStruct("ptr_data", "int_data")
class CXSourceLocation(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
}

@CNaturalStruct("ptr_data", "begin_int_data", "end_int_data")
class CXSourceRange(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var begin_int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
    var end_int_data: Int
        get() = memberAt<IntVar>(20).value
        set(value) { memberAt<IntVar>(20).value = value }
    
}

@CNaturalStruct("count", "ranges")
class CXSourceRangeList(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var count: Int
        get() = memberAt<IntVar>(0).value
        set(value) { memberAt<IntVar>(0).value = value }
    
    var ranges: CPointer<CXSourceRange>?
        get() = memberAt<CPointerVar<CXSourceRange>>(8).value
        set(value) { memberAt<CPointerVar<CXSourceRange>>(8).value = value }
    
}

@CNaturalStruct("kind", "amount")
class CXTUResourceUsageEntry(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var kind: CXTUResourceUsageKind
        get() = memberAt<CXTUResourceUsageKind.Var>(0).value
        set(value) { memberAt<CXTUResourceUsageKind.Var>(0).value = value }
    
    var amount: Long
        get() = memberAt<LongVar>(8).value
        set(value) { memberAt<LongVar>(8).value = value }
    
}

@CNaturalStruct("data", "numEntries", "entries")
class CXTUResourceUsage(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var numEntries: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
    var entries: CPointer<CXTUResourceUsageEntry>?
        get() = memberAt<CPointerVar<CXTUResourceUsageEntry>>(16).value
        set(value) { memberAt<CPointerVar<CXTUResourceUsageEntry>>(16).value = value }
    
}

@CNaturalStruct("kind", "xdata", "data")
class CXCursor(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(32, 8)
    
    var kind: CXCursorKind
        get() = memberAt<CXCursorKind.Var>(0).value
        set(value) { memberAt<CXCursorKind.Var>(0).value = value }
    
    var xdata: Int
        get() = memberAt<IntVar>(4).value
        set(value) { memberAt<IntVar>(4).value = value }
    
    @CLength(3)
    val data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("Platform", "Introduced", "Deprecated", "Obsoleted", "Unavailable", "Message")
class CXPlatformAvailability(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(72, 8)
    
    val Platform: CXString
        get() = memberAt(0)
    
    val Introduced: CXVersion
        get() = memberAt(16)
    
    val Deprecated: CXVersion
        get() = memberAt(28)
    
    val Obsoleted: CXVersion
        get() = memberAt(40)
    
    var Unavailable: Int
        get() = memberAt<IntVar>(52).value
        set(value) { memberAt<IntVar>(52).value = value }
    
    val Message: CXString
        get() = memberAt(56)
    
}

class CXCursorSetImpl(override val rawPtr: NativePtr) : COpaque

@CNaturalStruct("kind", "data")
class CXType(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var kind: CXTypeKind
        get() = memberAt<CXTypeKind.Var>(0).value
        set(value) { memberAt<CXTypeKind.Var>(0).value = value }
    
    @CLength(2)
    val data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(8)
    
}

@CNaturalStruct("int_data", "ptr_data")
class CXToken(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    @CLength(4)
    val int_data: CArrayPointer<IntVar>
        get() = arrayMemberAt(0)
    
    var ptr_data: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(16).value
        set(value) { memberAt<COpaquePointerVar>(16).value = value }
    
}

@CNaturalStruct("CursorKind", "CompletionString")
class CXCompletionResult(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var CursorKind: CXCursorKind
        get() = memberAt<CXCursorKind.Var>(0).value
        set(value) { memberAt<CXCursorKind.Var>(0).value = value }
    
    var CompletionString: CXCompletionString?
        get() = memberAt<CXCompletionStringVar>(8).value
        set(value) { memberAt<CXCompletionStringVar>(8).value = value }
    
}

@CNaturalStruct("Results", "NumResults")
class CXCodeCompleteResults(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var Results: CPointer<CXCompletionResult>?
        get() = memberAt<CPointerVar<CXCompletionResult>>(0).value
        set(value) { memberAt<CPointerVar<CXCompletionResult>>(0).value = value }
    
    var NumResults: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
}

@CNaturalStruct("context", "visit")
class CXCursorAndRangeVisitor(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var context: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(0).value
        set(value) { memberAt<COpaquePointerVar>(0).value = value }
    
    var visit: CPointer<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>?
        get() = memberAt<CPointerVar<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>>(8).value
        set(value) { memberAt<CPointerVar<CFunction<(COpaquePointer?, CValue<CXCursor>, CValue<CXSourceRange>) -> CXVisitorResult>>>(8).value = value }
    
}

@CNaturalStruct("ptr_data", "int_data")
class CXIdxLoc(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    @CLength(2)
    val ptr_data: CArrayPointer<COpaquePointerVar>
        get() = arrayMemberAt(0)
    
    var int_data: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
}

@CNaturalStruct("hashLoc", "filename", "file", "isImport", "isAngled", "isModuleImport")
class CXIdxIncludedFileInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(56, 8)
    
    val hashLoc: CXIdxLoc
        get() = memberAt(0)
    
    var filename: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(24).value
        set(value) { memberAt<CPointerVar<ByteVar>>(24).value = value }
    
    var file: CXFile?
        get() = memberAt<CXFileVar>(32).value
        set(value) { memberAt<CXFileVar>(32).value = value }
    
    var isImport: Int
        get() = memberAt<IntVar>(40).value
        set(value) { memberAt<IntVar>(40).value = value }
    
    var isAngled: Int
        get() = memberAt<IntVar>(44).value
        set(value) { memberAt<IntVar>(44).value = value }
    
    var isModuleImport: Int
        get() = memberAt<IntVar>(48).value
        set(value) { memberAt<IntVar>(48).value = value }
    
}

@CNaturalStruct("file", "module", "loc", "isImplicit")
class CXIdxImportedASTFileInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(48, 8)
    
    var file: CXFile?
        get() = memberAt<CXFileVar>(0).value
        set(value) { memberAt<CXFileVar>(0).value = value }
    
    var module: CXModule?
        get() = memberAt<CXModuleVar>(8).value
        set(value) { memberAt<CXModuleVar>(8).value = value }
    
    val loc: CXIdxLoc
        get() = memberAt(16)
    
    var isImplicit: Int
        get() = memberAt<IntVar>(40).value
        set(value) { memberAt<IntVar>(40).value = value }
    
}

@CNaturalStruct("kind", "cursor", "loc")
class CXIdxAttrInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var kind: CXIdxAttrKind
        get() = memberAt<CXIdxAttrKindVar>(0).value
        set(value) { memberAt<CXIdxAttrKindVar>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

@CNaturalStruct("kind", "templateKind", "lang", "name", "USR", "cursor", "attributes", "numAttributes")
class CXIdxEntityInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(80, 8)
    
    var kind: CXIdxEntityKind
        get() = memberAt<CXIdxEntityKind.Var>(0).value
        set(value) { memberAt<CXIdxEntityKind.Var>(0).value = value }
    
    var templateKind: CXIdxEntityCXXTemplateKind
        get() = memberAt<CXIdxEntityCXXTemplateKindVar>(4).value
        set(value) { memberAt<CXIdxEntityCXXTemplateKindVar>(4).value = value }
    
    var lang: CXIdxEntityLanguage
        get() = memberAt<CXIdxEntityLanguageVar>(8).value
        set(value) { memberAt<CXIdxEntityLanguageVar>(8).value = value }
    
    var name: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(16).value
        set(value) { memberAt<CPointerVar<ByteVar>>(16).value = value }
    
    var USR: CPointer<ByteVar>?
        get() = memberAt<CPointerVar<ByteVar>>(24).value
        set(value) { memberAt<CPointerVar<ByteVar>>(24).value = value }
    
    val cursor: CXCursor
        get() = memberAt(32)
    
    var attributes: CPointer<CPointerVar<CXIdxAttrInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(64).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(64).value = value }
    
    var numAttributes: Int
        get() = memberAt<IntVar>(72).value
        set(value) { memberAt<IntVar>(72).value = value }
    
}

@CNaturalStruct("cursor")
class CXIdxContainerInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(32, 8)
    
    val cursor: CXCursor
        get() = memberAt(0)
    
}

@CNaturalStruct("attrInfo", "objcClass", "classCursor", "classLoc")
class CXIdxIBOutletCollectionAttrInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(72, 8)
    
    var attrInfo: CPointer<CXIdxAttrInfo>?
        get() = memberAt<CPointerVar<CXIdxAttrInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxAttrInfo>>(0).value = value }
    
    var objcClass: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
    
}

@CNaturalStruct("entityInfo", "cursor", "loc", "semanticContainer", "lexicalContainer", "isRedeclaration", "isDefinition", "isContainer", "declAsContainer", "isImplicit", "attributes", "numAttributes", "flags")
class CXIdxDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(128, 8)
    
    var entityInfo: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    var semanticContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(64).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(64).value = value }
    
    var lexicalContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(72).value = value }
    
    var isRedeclaration: Int
        get() = memberAt<IntVar>(80).value
        set(value) { memberAt<IntVar>(80).value = value }
    
    var isDefinition: Int
        get() = memberAt<IntVar>(84).value
        set(value) { memberAt<IntVar>(84).value = value }
    
    var isContainer: Int
        get() = memberAt<IntVar>(88).value
        set(value) { memberAt<IntVar>(88).value = value }
    
    var declAsContainer: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(96).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(96).value = value }
    
    var isImplicit: Int
        get() = memberAt<IntVar>(104).value
        set(value) { memberAt<IntVar>(104).value = value }
    
    var attributes: CPointer<CPointerVar<CXIdxAttrInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(112).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxAttrInfo>>>(112).value = value }
    
    var numAttributes: Int
        get() = memberAt<IntVar>(120).value
        set(value) { memberAt<IntVar>(120).value = value }
    
    var flags: Int
        get() = memberAt<IntVar>(124).value
        set(value) { memberAt<IntVar>(124).value = value }
    
}

@CNaturalStruct("declInfo", "kind")
class CXIdxObjCContainerDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var kind: CXIdxObjCContainerKind
        get() = memberAt<CXIdxObjCContainerKindVar>(8).value
        set(value) { memberAt<CXIdxObjCContainerKindVar>(8).value = value }
    
}

@CNaturalStruct("base", "cursor", "loc")
class CXIdxBaseClassInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var base: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

@CNaturalStruct("protocol", "cursor", "loc")
class CXIdxObjCProtocolRefInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var protocol: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
}

@CNaturalStruct("protocols", "numProtocols")
class CXIdxObjCProtocolRefListInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(16, 8)
    
    var protocols: CPointer<CPointerVar<CXIdxObjCProtocolRefInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxObjCProtocolRefInfo>>>(0).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxObjCProtocolRefInfo>>>(0).value = value }
    
    var numProtocols: Int
        get() = memberAt<IntVar>(8).value
        set(value) { memberAt<IntVar>(8).value = value }
    
}

@CNaturalStruct("containerInfo", "superInfo", "protocols")
class CXIdxObjCInterfaceDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var containerInfo: CPointer<CXIdxObjCContainerDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value = value }
    
    var superInfo: CPointer<CXIdxBaseClassInfo>?
        get() = memberAt<CPointerVar<CXIdxBaseClassInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxBaseClassInfo>>(8).value = value }
    
    var protocols: CPointer<CXIdxObjCProtocolRefListInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(16).value
        set(value) { memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(16).value = value }
    
}

@CNaturalStruct("containerInfo", "objcClass", "classCursor", "classLoc", "protocols")
class CXIdxObjCCategoryDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(80, 8)
    
    var containerInfo: CPointer<CXIdxObjCContainerDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxObjCContainerDeclInfo>>(0).value = value }
    
    var objcClass: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    val classCursor: CXCursor
        get() = memberAt(16)
    
    val classLoc: CXIdxLoc
        get() = memberAt(48)
    
    var protocols: CPointer<CXIdxObjCProtocolRefListInfo>?
        get() = memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxObjCProtocolRefListInfo>>(72).value = value }
    
}

@CNaturalStruct("declInfo", "getter", "setter")
class CXIdxObjCPropertyDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var getter: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(8).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(8).value = value }
    
    var setter: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(16).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(16).value = value }
    
}

@CNaturalStruct("declInfo", "bases", "numBases")
class CXIdxCXXClassDeclInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(24, 8)
    
    var declInfo: CPointer<CXIdxDeclInfo>?
        get() = memberAt<CPointerVar<CXIdxDeclInfo>>(0).value
        set(value) { memberAt<CPointerVar<CXIdxDeclInfo>>(0).value = value }
    
    var bases: CPointer<CPointerVar<CXIdxBaseClassInfo>>?
        get() = memberAt<CPointerVar<CPointerVar<CXIdxBaseClassInfo>>>(8).value
        set(value) { memberAt<CPointerVar<CPointerVar<CXIdxBaseClassInfo>>>(8).value = value }
    
    var numBases: Int
        get() = memberAt<IntVar>(16).value
        set(value) { memberAt<IntVar>(16).value = value }
    
}

@CNaturalStruct("kind", "cursor", "loc", "referencedEntity", "parentEntity", "container")
class CXIdxEntityRefInfo(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(88, 8)
    
    var kind: CXIdxEntityRefKind
        get() = memberAt<CXIdxEntityRefKindVar>(0).value
        set(value) { memberAt<CXIdxEntityRefKindVar>(0).value = value }
    
    val cursor: CXCursor
        get() = memberAt(8)
    
    val loc: CXIdxLoc
        get() = memberAt(40)
    
    var referencedEntity: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(64).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(64).value = value }
    
    var parentEntity: CPointer<CXIdxEntityInfo>?
        get() = memberAt<CPointerVar<CXIdxEntityInfo>>(72).value
        set(value) { memberAt<CPointerVar<CXIdxEntityInfo>>(72).value = value }
    
    var container: CPointer<CXIdxContainerInfo>?
        get() = memberAt<CPointerVar<CXIdxContainerInfo>>(80).value
        set(value) { memberAt<CPointerVar<CXIdxContainerInfo>>(80).value = value }
    
}

@CNaturalStruct("abortQuery", "diagnostic", "enteredMainFile", "ppIncludedFile", "importedASTFile", "startedTranslationUnit", "indexDeclaration", "indexEntityReference")
class IndexerCallbacks(override val rawPtr: NativePtr) : CStructVar() {
    
    companion object : Type(64, 8)
    
    var abortQuery: CPointer<CFunction<(CXClientData?, COpaquePointer?) -> Int>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> Int>>>(0).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> Int>>>(0).value = value }
    
    var diagnostic: CPointer<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>>(8).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CXDiagnosticSet?, COpaquePointer?) -> Unit>>>(8).value = value }
    
    var enteredMainFile: CPointer<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>>(16).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CXFile?, COpaquePointer?) -> CXIdxClientFile?>>>(16).value = value }
    
    var ppIncludedFile: CPointer<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>>(24).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxIncludedFileInfo>?) -> CXIdxClientFile?>>>(24).value = value }
    
    var importedASTFile: CPointer<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>>(32).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxImportedASTFileInfo>?) -> CXIdxClientASTFile?>>>(32).value = value }
    
    var startedTranslationUnit: CPointer<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>>(40).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, COpaquePointer?) -> CXIdxClientContainer?>>>(40).value = value }
    
    var indexDeclaration: CPointer<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>>(48).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxDeclInfo>?) -> Unit>>>(48).value = value }
    
    var indexEntityReference: CPointer<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>?
        get() = memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>>(56).value
        set(value) { memberAt<CPointerVar<CFunction<(CXClientData?, CPointer<CXIdxEntityRefInfo>?) -> Unit>>>(56).value = value }
    
}

typealias clockid_tVar = IntVarOf<clockid_t>
typealias clockid_t = Int

val _CLOCK_REALTIME: clockid_t = 0
val _CLOCK_MONOTONIC: clockid_t = 6
val _CLOCK_MONOTONIC_RAW: clockid_t = 4
val _CLOCK_MONOTONIC_RAW_APPROX: clockid_t = 5
val _CLOCK_UPTIME_RAW: clockid_t = 8
val _CLOCK_UPTIME_RAW_APPROX: clockid_t = 9
val _CLOCK_PROCESS_CPUTIME_ID: clockid_t = 12
val _CLOCK_THREAD_CPUTIME_ID: clockid_t = 16

enum class CXErrorCode(override val value: Int) : CEnum {
    CXError_Success(0),
    CXError_Failure(1),
    CXError_Crashed(2),
    CXError_InvalidArguments(3),
    CXError_ASTReadError(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXErrorCode.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXErrorCode
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXAvailabilityKind(override val value: Int) : CEnum {
    CXAvailability_Available(0),
    CXAvailability_Deprecated(1),
    CXAvailability_NotAvailable(2),
    CXAvailability_NotAccessible(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXAvailabilityKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXAvailabilityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXGlobalOptFlagsVar = IntVarOf<CXGlobalOptFlags>
typealias CXGlobalOptFlags = Int

val CXGlobalOpt_None: CXGlobalOptFlags = 0
val CXGlobalOpt_ThreadBackgroundPriorityForIndexing: CXGlobalOptFlags = 1
val CXGlobalOpt_ThreadBackgroundPriorityForEditing: CXGlobalOptFlags = 2
val CXGlobalOpt_ThreadBackgroundPriorityForAll: CXGlobalOptFlags = 3

enum class CXDiagnosticSeverity(override val value: Int) : CEnum {
    CXDiagnostic_Ignored(0),
    CXDiagnostic_Note(1),
    CXDiagnostic_Warning(2),
    CXDiagnostic_Error(3),
    CXDiagnostic_Fatal(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXDiagnosticSeverity.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXDiagnosticSeverity
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLoadDiag_Error(override val value: Int) : CEnum {
    CXLoadDiag_None(0),
    CXLoadDiag_Unknown(1),
    CXLoadDiag_CannotLoad(2),
    CXLoadDiag_InvalidFile(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLoadDiag_Error.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXLoadDiag_Error
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXDiagnosticDisplayOptionsVar = IntVarOf<CXDiagnosticDisplayOptions>
typealias CXDiagnosticDisplayOptions = Int

val CXDiagnostic_DisplaySourceLocation: CXDiagnosticDisplayOptions = 1
val CXDiagnostic_DisplayColumn: CXDiagnosticDisplayOptions = 2
val CXDiagnostic_DisplaySourceRanges: CXDiagnosticDisplayOptions = 4
val CXDiagnostic_DisplayOption: CXDiagnosticDisplayOptions = 8
val CXDiagnostic_DisplayCategoryId: CXDiagnosticDisplayOptions = 16
val CXDiagnostic_DisplayCategoryName: CXDiagnosticDisplayOptions = 32

typealias CXTranslationUnit_FlagsVar = IntVarOf<CXTranslationUnit_Flags>
typealias CXTranslationUnit_Flags = Int

val CXTranslationUnit_None: CXTranslationUnit_Flags = 0
val CXTranslationUnit_DetailedPreprocessingRecord: CXTranslationUnit_Flags = 1
val CXTranslationUnit_Incomplete: CXTranslationUnit_Flags = 2
val CXTranslationUnit_PrecompiledPreamble: CXTranslationUnit_Flags = 4
val CXTranslationUnit_CacheCompletionResults: CXTranslationUnit_Flags = 8
val CXTranslationUnit_ForSerialization: CXTranslationUnit_Flags = 16
val CXTranslationUnit_CXXChainedPCH: CXTranslationUnit_Flags = 32
val CXTranslationUnit_SkipFunctionBodies: CXTranslationUnit_Flags = 64
val CXTranslationUnit_IncludeBriefCommentsInCodeCompletion: CXTranslationUnit_Flags = 128
val CXTranslationUnit_CreatePreambleOnFirstParse: CXTranslationUnit_Flags = 256
val CXTranslationUnit_KeepGoing: CXTranslationUnit_Flags = 512

typealias CXSaveTranslationUnit_FlagsVar = IntVarOf<CXSaveTranslationUnit_Flags>
typealias CXSaveTranslationUnit_Flags = Int

val CXSaveTranslationUnit_None: CXSaveTranslationUnit_Flags = 0

enum class CXSaveError(override val value: Int) : CEnum {
    CXSaveError_None(0),
    CXSaveError_Unknown(1),
    CXSaveError_TranslationErrors(2),
    CXSaveError_InvalidTU(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXSaveError.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXSaveError
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXReparse_FlagsVar = IntVarOf<CXReparse_Flags>
typealias CXReparse_Flags = Int

val CXReparse_None: CXReparse_Flags = 0

enum class CXTUResourceUsageKind(override val value: Int) : CEnum {
    CXTUResourceUsage_AST(1),
    CXTUResourceUsage_Identifiers(2),
    CXTUResourceUsage_Selectors(3),
    CXTUResourceUsage_GlobalCompletionResults(4),
    CXTUResourceUsage_SourceManagerContentCache(5),
    CXTUResourceUsage_AST_SideTables(6),
    CXTUResourceUsage_SourceManager_Membuffer_Malloc(7),
    CXTUResourceUsage_SourceManager_Membuffer_MMap(8),
    CXTUResourceUsage_ExternalASTSource_Membuffer_Malloc(9),
    CXTUResourceUsage_ExternalASTSource_Membuffer_MMap(10),
    CXTUResourceUsage_Preprocessor(11),
    CXTUResourceUsage_PreprocessingRecord(12),
    CXTUResourceUsage_SourceManager_DataStructures(13),
    CXTUResourceUsage_Preprocessor_HeaderSearch(14),
    CXTUResourceUsage_MEMORY_IN_BYTES_BEGIN(1),
    CXTUResourceUsage_MEMORY_IN_BYTES_END(14),
    CXTUResourceUsage_First(1),
    CXTUResourceUsage_Last(14),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTUResourceUsageKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXTUResourceUsageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCursorKind(override val value: Int) : CEnum {
    CXCursor_UnexposedDecl(1),
    CXCursor_StructDecl(2),
    CXCursor_UnionDecl(3),
    CXCursor_ClassDecl(4),
    CXCursor_EnumDecl(5),
    CXCursor_FieldDecl(6),
    CXCursor_EnumConstantDecl(7),
    CXCursor_FunctionDecl(8),
    CXCursor_VarDecl(9),
    CXCursor_ParmDecl(10),
    CXCursor_ObjCInterfaceDecl(11),
    CXCursor_ObjCCategoryDecl(12),
    CXCursor_ObjCProtocolDecl(13),
    CXCursor_ObjCPropertyDecl(14),
    CXCursor_ObjCIvarDecl(15),
    CXCursor_ObjCInstanceMethodDecl(16),
    CXCursor_ObjCClassMethodDecl(17),
    CXCursor_ObjCImplementationDecl(18),
    CXCursor_ObjCCategoryImplDecl(19),
    CXCursor_TypedefDecl(20),
    CXCursor_CXXMethod(21),
    CXCursor_Namespace(22),
    CXCursor_LinkageSpec(23),
    CXCursor_Constructor(24),
    CXCursor_Destructor(25),
    CXCursor_ConversionFunction(26),
    CXCursor_TemplateTypeParameter(27),
    CXCursor_NonTypeTemplateParameter(28),
    CXCursor_TemplateTemplateParameter(29),
    CXCursor_FunctionTemplate(30),
    CXCursor_ClassTemplate(31),
    CXCursor_ClassTemplatePartialSpecialization(32),
    CXCursor_NamespaceAlias(33),
    CXCursor_UsingDirective(34),
    CXCursor_UsingDeclaration(35),
    CXCursor_TypeAliasDecl(36),
    CXCursor_ObjCSynthesizeDecl(37),
    CXCursor_ObjCDynamicDecl(38),
    CXCursor_CXXAccessSpecifier(39),
    CXCursor_FirstDecl(1),
    CXCursor_LastDecl(39),
    CXCursor_FirstRef(40),
    CXCursor_ObjCSuperClassRef(40),
    CXCursor_ObjCProtocolRef(41),
    CXCursor_ObjCClassRef(42),
    CXCursor_TypeRef(43),
    CXCursor_CXXBaseSpecifier(44),
    CXCursor_TemplateRef(45),
    CXCursor_NamespaceRef(46),
    CXCursor_MemberRef(47),
    CXCursor_LabelRef(48),
    CXCursor_OverloadedDeclRef(49),
    CXCursor_VariableRef(50),
    CXCursor_LastRef(50),
    CXCursor_FirstInvalid(70),
    CXCursor_InvalidFile(70),
    CXCursor_NoDeclFound(71),
    CXCursor_NotImplemented(72),
    CXCursor_InvalidCode(73),
    CXCursor_LastInvalid(73),
    CXCursor_FirstExpr(100),
    CXCursor_UnexposedExpr(100),
    CXCursor_DeclRefExpr(101),
    CXCursor_MemberRefExpr(102),
    CXCursor_CallExpr(103),
    CXCursor_ObjCMessageExpr(104),
    CXCursor_BlockExpr(105),
    CXCursor_IntegerLiteral(106),
    CXCursor_FloatingLiteral(107),
    CXCursor_ImaginaryLiteral(108),
    CXCursor_StringLiteral(109),
    CXCursor_CharacterLiteral(110),
    CXCursor_ParenExpr(111),
    CXCursor_UnaryOperator(112),
    CXCursor_ArraySubscriptExpr(113),
    CXCursor_BinaryOperator(114),
    CXCursor_CompoundAssignOperator(115),
    CXCursor_ConditionalOperator(116),
    CXCursor_CStyleCastExpr(117),
    CXCursor_CompoundLiteralExpr(118),
    CXCursor_InitListExpr(119),
    CXCursor_AddrLabelExpr(120),
    CXCursor_StmtExpr(121),
    CXCursor_GenericSelectionExpr(122),
    CXCursor_GNUNullExpr(123),
    CXCursor_CXXStaticCastExpr(124),
    CXCursor_CXXDynamicCastExpr(125),
    CXCursor_CXXReinterpretCastExpr(126),
    CXCursor_CXXConstCastExpr(127),
    CXCursor_CXXFunctionalCastExpr(128),
    CXCursor_CXXTypeidExpr(129),
    CXCursor_CXXBoolLiteralExpr(130),
    CXCursor_CXXNullPtrLiteralExpr(131),
    CXCursor_CXXThisExpr(132),
    CXCursor_CXXThrowExpr(133),
    CXCursor_CXXNewExpr(134),
    CXCursor_CXXDeleteExpr(135),
    CXCursor_UnaryExpr(136),
    CXCursor_ObjCStringLiteral(137),
    CXCursor_ObjCEncodeExpr(138),
    CXCursor_ObjCSelectorExpr(139),
    CXCursor_ObjCProtocolExpr(140),
    CXCursor_ObjCBridgedCastExpr(141),
    CXCursor_PackExpansionExpr(142),
    CXCursor_SizeOfPackExpr(143),
    CXCursor_LambdaExpr(144),
    CXCursor_ObjCBoolLiteralExpr(145),
    CXCursor_ObjCSelfExpr(146),
    CXCursor_OMPArraySectionExpr(147),
    CXCursor_ObjCAvailabilityCheckExpr(148),
    CXCursor_LastExpr(148),
    CXCursor_FirstStmt(200),
    CXCursor_UnexposedStmt(200),
    CXCursor_LabelStmt(201),
    CXCursor_CompoundStmt(202),
    CXCursor_CaseStmt(203),
    CXCursor_DefaultStmt(204),
    CXCursor_IfStmt(205),
    CXCursor_SwitchStmt(206),
    CXCursor_WhileStmt(207),
    CXCursor_DoStmt(208),
    CXCursor_ForStmt(209),
    CXCursor_GotoStmt(210),
    CXCursor_IndirectGotoStmt(211),
    CXCursor_ContinueStmt(212),
    CXCursor_BreakStmt(213),
    CXCursor_ReturnStmt(214),
    CXCursor_GCCAsmStmt(215),
    CXCursor_AsmStmt(215),
    CXCursor_ObjCAtTryStmt(216),
    CXCursor_ObjCAtCatchStmt(217),
    CXCursor_ObjCAtFinallyStmt(218),
    CXCursor_ObjCAtThrowStmt(219),
    CXCursor_ObjCAtSynchronizedStmt(220),
    CXCursor_ObjCAutoreleasePoolStmt(221),
    CXCursor_ObjCForCollectionStmt(222),
    CXCursor_CXXCatchStmt(223),
    CXCursor_CXXTryStmt(224),
    CXCursor_CXXForRangeStmt(225),
    CXCursor_SEHTryStmt(226),
    CXCursor_SEHExceptStmt(227),
    CXCursor_SEHFinallyStmt(228),
    CXCursor_MSAsmStmt(229),
    CXCursor_NullStmt(230),
    CXCursor_DeclStmt(231),
    CXCursor_OMPParallelDirective(232),
    CXCursor_OMPSimdDirective(233),
    CXCursor_OMPForDirective(234),
    CXCursor_OMPSectionsDirective(235),
    CXCursor_OMPSectionDirective(236),
    CXCursor_OMPSingleDirective(237),
    CXCursor_OMPParallelForDirective(238),
    CXCursor_OMPParallelSectionsDirective(239),
    CXCursor_OMPTaskDirective(240),
    CXCursor_OMPMasterDirective(241),
    CXCursor_OMPCriticalDirective(242),
    CXCursor_OMPTaskyieldDirective(243),
    CXCursor_OMPBarrierDirective(244),
    CXCursor_OMPTaskwaitDirective(245),
    CXCursor_OMPFlushDirective(246),
    CXCursor_SEHLeaveStmt(247),
    CXCursor_OMPOrderedDirective(248),
    CXCursor_OMPAtomicDirective(249),
    CXCursor_OMPForSimdDirective(250),
    CXCursor_OMPParallelForSimdDirective(251),
    CXCursor_OMPTargetDirective(252),
    CXCursor_OMPTeamsDirective(253),
    CXCursor_OMPTaskgroupDirective(254),
    CXCursor_OMPCancellationPointDirective(255),
    CXCursor_OMPCancelDirective(256),
    CXCursor_OMPTargetDataDirective(257),
    CXCursor_OMPTaskLoopDirective(258),
    CXCursor_OMPTaskLoopSimdDirective(259),
    CXCursor_OMPDistributeDirective(260),
    CXCursor_OMPTargetEnterDataDirective(261),
    CXCursor_OMPTargetExitDataDirective(262),
    CXCursor_OMPTargetParallelDirective(263),
    CXCursor_OMPTargetParallelForDirective(264),
    CXCursor_OMPTargetUpdateDirective(265),
    CXCursor_OMPDistributeParallelForDirective(266),
    CXCursor_OMPDistributeParallelForSimdDirective(267),
    CXCursor_OMPDistributeSimdDirective(268),
    CXCursor_OMPTargetParallelForSimdDirective(269),
    CXCursor_LastStmt(269),
    CXCursor_TranslationUnit(300),
    CXCursor_FirstAttr(400),
    CXCursor_UnexposedAttr(400),
    CXCursor_IBActionAttr(401),
    CXCursor_IBOutletAttr(402),
    CXCursor_IBOutletCollectionAttr(403),
    CXCursor_CXXFinalAttr(404),
    CXCursor_CXXOverrideAttr(405),
    CXCursor_AnnotateAttr(406),
    CXCursor_AsmLabelAttr(407),
    CXCursor_PackedAttr(408),
    CXCursor_PureAttr(409),
    CXCursor_ConstAttr(410),
    CXCursor_NoDuplicateAttr(411),
    CXCursor_CUDAConstantAttr(412),
    CXCursor_CUDADeviceAttr(413),
    CXCursor_CUDAGlobalAttr(414),
    CXCursor_CUDAHostAttr(415),
    CXCursor_CUDASharedAttr(416),
    CXCursor_VisibilityAttr(417),
    CXCursor_DLLExport(418),
    CXCursor_DLLImport(419),
    CXCursor_LastAttr(419),
    CXCursor_PreprocessingDirective(500),
    CXCursor_MacroDefinition(501),
    CXCursor_MacroExpansion(502),
    CXCursor_MacroInstantiation(502),
    CXCursor_InclusionDirective(503),
    CXCursor_FirstPreprocessing(500),
    CXCursor_LastPreprocessing(503),
    CXCursor_ModuleImportDecl(600),
    CXCursor_TypeAliasTemplateDecl(601),
    CXCursor_StaticAssert(602),
    CXCursor_FirstExtraDecl(600),
    CXCursor_LastExtraDecl(602),
    CXCursor_OverloadCandidate(700),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCursorKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXCursorKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLinkageKind(override val value: Int) : CEnum {
    CXLinkage_Invalid(0),
    CXLinkage_NoLinkage(1),
    CXLinkage_Internal(2),
    CXLinkage_UniqueExternal(3),
    CXLinkage_External(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLinkageKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXLinkageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXVisibilityKind(override val value: Int) : CEnum {
    CXVisibility_Invalid(0),
    CXVisibility_Hidden(1),
    CXVisibility_Protected(2),
    CXVisibility_Default(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXVisibilityKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXVisibilityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXLanguageKind(override val value: Int) : CEnum {
    CXLanguage_Invalid(0),
    CXLanguage_C(1),
    CXLanguage_ObjC(2),
    CXLanguage_CPlusPlus(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CXLanguageKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXLanguageKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTypeKind(override val value: Int) : CEnum {
    CXType_Invalid(0),
    CXType_Unexposed(1),
    CXType_Void(2),
    CXType_Bool(3),
    CXType_Char_U(4),
    CXType_UChar(5),
    CXType_Char16(6),
    CXType_Char32(7),
    CXType_UShort(8),
    CXType_UInt(9),
    CXType_ULong(10),
    CXType_ULongLong(11),
    CXType_UInt128(12),
    CXType_Char_S(13),
    CXType_SChar(14),
    CXType_WChar(15),
    CXType_Short(16),
    CXType_Int(17),
    CXType_Long(18),
    CXType_LongLong(19),
    CXType_Int128(20),
    CXType_Float(21),
    CXType_Double(22),
    CXType_LongDouble(23),
    CXType_NullPtr(24),
    CXType_Overload(25),
    CXType_Dependent(26),
    CXType_ObjCId(27),
    CXType_ObjCClass(28),
    CXType_ObjCSel(29),
    CXType_Float128(30),
    CXType_FirstBuiltin(2),
    CXType_LastBuiltin(29),
    CXType_Complex(100),
    CXType_Pointer(101),
    CXType_BlockPointer(102),
    CXType_LValueReference(103),
    CXType_RValueReference(104),
    CXType_Record(105),
    CXType_Enum(106),
    CXType_Typedef(107),
    CXType_ObjCInterface(108),
    CXType_ObjCObjectPointer(109),
    CXType_FunctionNoProto(110),
    CXType_FunctionProto(111),
    CXType_ConstantArray(112),
    CXType_Vector(113),
    CXType_IncompleteArray(114),
    CXType_VariableArray(115),
    CXType_DependentSizedArray(116),
    CXType_MemberPointer(117),
    CXType_Auto(118),
    CXType_Elaborated(119),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTypeKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXTypeKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCallingConv(override val value: Int) : CEnum {
    CXCallingConv_Default(0),
    CXCallingConv_C(1),
    CXCallingConv_X86StdCall(2),
    CXCallingConv_X86FastCall(3),
    CXCallingConv_X86ThisCall(4),
    CXCallingConv_X86Pascal(5),
    CXCallingConv_AAPCS(6),
    CXCallingConv_AAPCS_VFP(7),
    CXCallingConv_IntelOclBicc(9),
    CXCallingConv_X86_64Win64(10),
    CXCallingConv_X86_64SysV(11),
    CXCallingConv_X86VectorCall(12),
    CXCallingConv_Swift(13),
    CXCallingConv_PreserveMost(14),
    CXCallingConv_PreserveAll(15),
    CXCallingConv_Invalid(100),
    CXCallingConv_Unexposed(200),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCallingConv.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXCallingConv
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXTemplateArgumentKind(override val value: Int) : CEnum {
    CXTemplateArgumentKind_Null(0),
    CXTemplateArgumentKind_Type(1),
    CXTemplateArgumentKind_Declaration(2),
    CXTemplateArgumentKind_NullPtr(3),
    CXTemplateArgumentKind_Integral(4),
    CXTemplateArgumentKind_Template(5),
    CXTemplateArgumentKind_TemplateExpansion(6),
    CXTemplateArgumentKind_Expression(7),
    CXTemplateArgumentKind_Pack(8),
    CXTemplateArgumentKind_Invalid(9),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTemplateArgumentKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXTemplateArgumentKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXTypeLayoutErrorVar = IntVarOf<CXTypeLayoutError>
typealias CXTypeLayoutError = Int

val CXTypeLayoutError_Invalid: CXTypeLayoutError = -1
val CXTypeLayoutError_Incomplete: CXTypeLayoutError = -2
val CXTypeLayoutError_Dependent: CXTypeLayoutError = -3
val CXTypeLayoutError_NotConstantSize: CXTypeLayoutError = -4
val CXTypeLayoutError_InvalidFieldName: CXTypeLayoutError = -5

typealias CXRefQualifierKindVar = IntVarOf<CXRefQualifierKind>
typealias CXRefQualifierKind = Int

val CXRefQualifier_None: CXRefQualifierKind = 0
val CXRefQualifier_LValue: CXRefQualifierKind = 1
val CXRefQualifier_RValue: CXRefQualifierKind = 2

enum class CX_CXXAccessSpecifier(override val value: Int) : CEnum {
    CX_CXXInvalidAccessSpecifier(0),
    CX_CXXPublic(1),
    CX_CXXProtected(2),
    CX_CXXPrivate(3),
    ;
    
    companion object {
        fun byValue(value: Int) = CX_CXXAccessSpecifier.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CX_CXXAccessSpecifier
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CX_StorageClass(override val value: Int) : CEnum {
    CX_SC_Invalid(0),
    CX_SC_None(1),
    CX_SC_Extern(2),
    CX_SC_Static(3),
    CX_SC_PrivateExtern(4),
    CX_SC_OpenCLWorkGroupLocal(5),
    CX_SC_Auto(6),
    CX_SC_Register(7),
    ;
    
    companion object {
        fun byValue(value: Int) = CX_StorageClass.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CX_StorageClass
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXChildVisitResult(override val value: Int) : CEnum {
    CXChildVisit_Break(0),
    CXChildVisit_Continue(1),
    CXChildVisit_Recurse(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXChildVisitResult.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXChildVisitResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXObjCPropertyAttrKindVar = IntVarOf<CXObjCPropertyAttrKind>
typealias CXObjCPropertyAttrKind = Int

val CXObjCPropertyAttr_noattr: CXObjCPropertyAttrKind = 0
val CXObjCPropertyAttr_readonly: CXObjCPropertyAttrKind = 1
val CXObjCPropertyAttr_getter: CXObjCPropertyAttrKind = 2
val CXObjCPropertyAttr_assign: CXObjCPropertyAttrKind = 4
val CXObjCPropertyAttr_readwrite: CXObjCPropertyAttrKind = 8
val CXObjCPropertyAttr_retain: CXObjCPropertyAttrKind = 16
val CXObjCPropertyAttr_copy: CXObjCPropertyAttrKind = 32
val CXObjCPropertyAttr_nonatomic: CXObjCPropertyAttrKind = 64
val CXObjCPropertyAttr_setter: CXObjCPropertyAttrKind = 128
val CXObjCPropertyAttr_atomic: CXObjCPropertyAttrKind = 256
val CXObjCPropertyAttr_weak: CXObjCPropertyAttrKind = 512
val CXObjCPropertyAttr_strong: CXObjCPropertyAttrKind = 1024
val CXObjCPropertyAttr_unsafe_unretained: CXObjCPropertyAttrKind = 2048
val CXObjCPropertyAttr_class: CXObjCPropertyAttrKind = 4096

typealias CXObjCDeclQualifierKindVar = IntVarOf<CXObjCDeclQualifierKind>
typealias CXObjCDeclQualifierKind = Int

val CXObjCDeclQualifier_None: CXObjCDeclQualifierKind = 0
val CXObjCDeclQualifier_In: CXObjCDeclQualifierKind = 1
val CXObjCDeclQualifier_Inout: CXObjCDeclQualifierKind = 2
val CXObjCDeclQualifier_Out: CXObjCDeclQualifierKind = 4
val CXObjCDeclQualifier_Bycopy: CXObjCDeclQualifierKind = 8
val CXObjCDeclQualifier_Byref: CXObjCDeclQualifierKind = 16
val CXObjCDeclQualifier_Oneway: CXObjCDeclQualifierKind = 32

typealias CXNameRefFlagsVar = IntVarOf<CXNameRefFlags>
typealias CXNameRefFlags = Int

val CXNameRange_WantQualifier: CXNameRefFlags = 1
val CXNameRange_WantTemplateArgs: CXNameRefFlags = 2
val CXNameRange_WantSinglePiece: CXNameRefFlags = 4

enum class CXTokenKind(override val value: Int) : CEnum {
    CXToken_Punctuation(0),
    CXToken_Keyword(1),
    CXToken_Identifier(2),
    CXToken_Literal(3),
    CXToken_Comment(4),
    ;
    
    companion object {
        fun byValue(value: Int) = CXTokenKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXTokenKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXCompletionChunkKind(override val value: Int) : CEnum {
    CXCompletionChunk_Optional(0),
    CXCompletionChunk_TypedText(1),
    CXCompletionChunk_Text(2),
    CXCompletionChunk_Placeholder(3),
    CXCompletionChunk_Informative(4),
    CXCompletionChunk_CurrentParameter(5),
    CXCompletionChunk_LeftParen(6),
    CXCompletionChunk_RightParen(7),
    CXCompletionChunk_LeftBracket(8),
    CXCompletionChunk_RightBracket(9),
    CXCompletionChunk_LeftBrace(10),
    CXCompletionChunk_RightBrace(11),
    CXCompletionChunk_LeftAngle(12),
    CXCompletionChunk_RightAngle(13),
    CXCompletionChunk_Comma(14),
    CXCompletionChunk_ResultType(15),
    CXCompletionChunk_Colon(16),
    CXCompletionChunk_SemiColon(17),
    CXCompletionChunk_Equal(18),
    CXCompletionChunk_HorizontalSpace(19),
    CXCompletionChunk_VerticalSpace(20),
    ;
    
    companion object {
        fun byValue(value: Int) = CXCompletionChunkKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXCompletionChunkKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXCodeComplete_FlagsVar = IntVarOf<CXCodeComplete_Flags>
typealias CXCodeComplete_Flags = Int

val CXCodeComplete_IncludeMacros: CXCodeComplete_Flags = 1
val CXCodeComplete_IncludeCodePatterns: CXCodeComplete_Flags = 2
val CXCodeComplete_IncludeBriefComments: CXCodeComplete_Flags = 4

typealias CXCompletionContextVar = IntVarOf<CXCompletionContext>
typealias CXCompletionContext = Int

val CXCompletionContext_Unexposed: CXCompletionContext = 0
val CXCompletionContext_AnyType: CXCompletionContext = 1
val CXCompletionContext_AnyValue: CXCompletionContext = 2
val CXCompletionContext_ObjCObjectValue: CXCompletionContext = 4
val CXCompletionContext_ObjCSelectorValue: CXCompletionContext = 8
val CXCompletionContext_CXXClassTypeValue: CXCompletionContext = 16
val CXCompletionContext_DotMemberAccess: CXCompletionContext = 32
val CXCompletionContext_ArrowMemberAccess: CXCompletionContext = 64
val CXCompletionContext_ObjCPropertyAccess: CXCompletionContext = 128
val CXCompletionContext_EnumTag: CXCompletionContext = 256
val CXCompletionContext_UnionTag: CXCompletionContext = 512
val CXCompletionContext_StructTag: CXCompletionContext = 1024
val CXCompletionContext_ClassTag: CXCompletionContext = 2048
val CXCompletionContext_Namespace: CXCompletionContext = 4096
val CXCompletionContext_NestedNameSpecifier: CXCompletionContext = 8192
val CXCompletionContext_ObjCInterface: CXCompletionContext = 16384
val CXCompletionContext_ObjCProtocol: CXCompletionContext = 32768
val CXCompletionContext_ObjCCategory: CXCompletionContext = 65536
val CXCompletionContext_ObjCInstanceMessage: CXCompletionContext = 131072
val CXCompletionContext_ObjCClassMessage: CXCompletionContext = 262144
val CXCompletionContext_ObjCSelectorName: CXCompletionContext = 524288
val CXCompletionContext_MacroName: CXCompletionContext = 1048576
val CXCompletionContext_NaturalLanguage: CXCompletionContext = 2097152
val CXCompletionContext_Unknown: CXCompletionContext = 4194303

enum class CXEvalResultKind(override val value: Int) : CEnum {
    CXEval_Int(1),
    CXEval_Float(2),
    CXEval_ObjCStrLiteral(3),
    CXEval_StrLiteral(4),
    CXEval_CFStr(5),
    CXEval_Other(6),
    CXEval_UnExposed(0),
    ;
    
    companion object {
        fun byValue(value: Int) = CXEvalResultKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXEvalResultKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXVisitorResult(override val value: Int) : CEnum {
    CXVisit_Break(0),
    CXVisit_Continue(1),
    ;
    
    companion object {
        fun byValue(value: Int) = CXVisitorResult.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXVisitorResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXResult(override val value: Int) : CEnum {
    CXResult_Success(0),
    CXResult_Invalid(1),
    CXResult_VisitBreak(2),
    ;
    
    companion object {
        fun byValue(value: Int) = CXResult.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXResult
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

enum class CXIdxEntityKind(override val value: Int) : CEnum {
    CXIdxEntity_Unexposed(0),
    CXIdxEntity_Typedef(1),
    CXIdxEntity_Function(2),
    CXIdxEntity_Variable(3),
    CXIdxEntity_Field(4),
    CXIdxEntity_EnumConstant(5),
    CXIdxEntity_ObjCClass(6),
    CXIdxEntity_ObjCProtocol(7),
    CXIdxEntity_ObjCCategory(8),
    CXIdxEntity_ObjCInstanceMethod(9),
    CXIdxEntity_ObjCClassMethod(10),
    CXIdxEntity_ObjCProperty(11),
    CXIdxEntity_ObjCIvar(12),
    CXIdxEntity_Enum(13),
    CXIdxEntity_Struct(14),
    CXIdxEntity_Union(15),
    CXIdxEntity_CXXClass(16),
    CXIdxEntity_CXXNamespace(17),
    CXIdxEntity_CXXNamespaceAlias(18),
    CXIdxEntity_CXXStaticVariable(19),
    CXIdxEntity_CXXStaticMethod(20),
    CXIdxEntity_CXXInstanceMethod(21),
    CXIdxEntity_CXXConstructor(22),
    CXIdxEntity_CXXDestructor(23),
    CXIdxEntity_CXXConversionFunction(24),
    CXIdxEntity_CXXTypeAlias(25),
    CXIdxEntity_CXXInterface(26),
    ;
    
    companion object {
        fun byValue(value: Int) = CXIdxEntityKind.values().find { it.value == value }!!
    }
    
    class Var(override val rawPtr: NativePtr) : CEnumVar() {
        companion object : Type(IntVar.size.toInt())
        var value: CXIdxEntityKind
            get() = byValue(this.reinterpret<IntVar>().value)
            set(value) { this.reinterpret<IntVar>().value = value.value }
    }
}

typealias CXIdxEntityLanguageVar = IntVarOf<CXIdxEntityLanguage>
typealias CXIdxEntityLanguage = Int

val CXIdxEntityLang_None: CXIdxEntityLanguage = 0
val CXIdxEntityLang_C: CXIdxEntityLanguage = 1
val CXIdxEntityLang_ObjC: CXIdxEntityLanguage = 2
val CXIdxEntityLang_CXX: CXIdxEntityLanguage = 3

typealias CXIdxEntityCXXTemplateKindVar = IntVarOf<CXIdxEntityCXXTemplateKind>
typealias CXIdxEntityCXXTemplateKind = Int

val CXIdxEntity_NonTemplate: CXIdxEntityCXXTemplateKind = 0
val CXIdxEntity_Template: CXIdxEntityCXXTemplateKind = 1
val CXIdxEntity_TemplatePartialSpecialization: CXIdxEntityCXXTemplateKind = 2
val CXIdxEntity_TemplateSpecialization: CXIdxEntityCXXTemplateKind = 3

typealias CXIdxAttrKindVar = IntVarOf<CXIdxAttrKind>
typealias CXIdxAttrKind = Int

val CXIdxAttr_Unexposed: CXIdxAttrKind = 0
val CXIdxAttr_IBAction: CXIdxAttrKind = 1
val CXIdxAttr_IBOutlet: CXIdxAttrKind = 2
val CXIdxAttr_IBOutletCollection: CXIdxAttrKind = 3

typealias CXIdxDeclInfoFlagsVar = IntVarOf<CXIdxDeclInfoFlags>
typealias CXIdxDeclInfoFlags = Int

val CXIdxDeclFlag_Skipped: CXIdxDeclInfoFlags = 1

typealias CXIdxObjCContainerKindVar = IntVarOf<CXIdxObjCContainerKind>
typealias CXIdxObjCContainerKind = Int

val CXIdxObjCContainer_ForwardRef: CXIdxObjCContainerKind = 0
val CXIdxObjCContainer_Interface: CXIdxObjCContainerKind = 1
val CXIdxObjCContainer_Implementation: CXIdxObjCContainerKind = 2

typealias CXIdxEntityRefKindVar = IntVarOf<CXIdxEntityRefKind>
typealias CXIdxEntityRefKind = Int

val CXIdxEntityRef_Direct: CXIdxEntityRefKind = 1
val CXIdxEntityRef_Implicit: CXIdxEntityRefKind = 2

typealias CXIndexOptFlagsVar = IntVarOf<CXIndexOptFlags>
typealias CXIndexOptFlags = Int

val CXIndexOpt_None: CXIndexOptFlags = 0
val CXIndexOpt_SuppressRedundantRefs: CXIndexOptFlags = 1
val CXIndexOpt_IndexFunctionLocalSymbols: CXIndexOptFlags = 2
val CXIndexOpt_IndexImplicitTemplateInstantiations: CXIndexOptFlags = 4
val CXIndexOpt_SuppressWarnings: CXIndexOptFlags = 8
val CXIndexOpt_SkipParsedBodiesInSession: CXIndexOptFlags = 16

typealias __int8_tVar = ByteVarOf<__int8_t>
typealias __int8_t = Byte

typealias __uint8_tVar = ByteVarOf<__uint8_t>
typealias __uint8_t = Byte

typealias __int16_tVar = ShortVarOf<__int16_t>
typealias __int16_t = Short

typealias __uint16_tVar = ShortVarOf<__uint16_t>
typealias __uint16_t = Short

typealias __int32_tVar = IntVarOf<__int32_t>
typealias __int32_t = Int

typealias __uint32_tVar = IntVarOf<__uint32_t>
typealias __uint32_t = Int

typealias __int64_tVar = LongVarOf<__int64_t>
typealias __int64_t = Long

typealias __uint64_tVar = LongVarOf<__uint64_t>
typealias __uint64_t = Long

typealias __darwin_intptr_tVar = LongVarOf<__darwin_intptr_t>
typealias __darwin_intptr_t = Long

typealias __darwin_natural_tVar = IntVarOf<__darwin_natural_t>
typealias __darwin_natural_t = Int

typealias __darwin_ct_rune_tVar = IntVarOf<__darwin_ct_rune_t>
typealias __darwin_ct_rune_t = Int

typealias __darwin_mbstate_t = __mbstate_t

typealias __darwin_ptrdiff_tVar = LongVarOf<__darwin_ptrdiff_t>
typealias __darwin_ptrdiff_t = Long

typealias __darwin_size_tVar = LongVarOf<__darwin_size_t>
typealias __darwin_size_t = Long

typealias __darwin_va_listVar = CPointerVarOf<__darwin_va_list>
typealias __darwin_va_list = CArrayPointer<__builtin_va_list>

typealias __darwin_wchar_tVar = IntVarOf<__darwin_wchar_t>
typealias __darwin_wchar_t = Int

typealias __darwin_rune_tVar = IntVarOf<__darwin_rune_t>
typealias __darwin_rune_t = __darwin_wchar_t

typealias __darwin_wint_tVar = IntVarOf<__darwin_wint_t>
typealias __darwin_wint_t = Int

typealias __darwin_clock_tVar = LongVarOf<__darwin_clock_t>
typealias __darwin_clock_t = Long

typealias __darwin_socklen_tVar = IntVarOf<__darwin_socklen_t>
typealias __darwin_socklen_t = __uint32_t

typealias __darwin_ssize_tVar = LongVarOf<__darwin_ssize_t>
typealias __darwin_ssize_t = Long

typealias __darwin_time_tVar = LongVarOf<__darwin_time_t>
typealias __darwin_time_t = Long

typealias __darwin_blkcnt_tVar = LongVarOf<__darwin_blkcnt_t>
typealias __darwin_blkcnt_t = __int64_t

typealias __darwin_blksize_tVar = IntVarOf<__darwin_blksize_t>
typealias __darwin_blksize_t = __int32_t

typealias __darwin_dev_tVar = IntVarOf<__darwin_dev_t>
typealias __darwin_dev_t = __int32_t

typealias __darwin_fsblkcnt_tVar = IntVarOf<__darwin_fsblkcnt_t>
typealias __darwin_fsblkcnt_t = Int

typealias __darwin_fsfilcnt_tVar = IntVarOf<__darwin_fsfilcnt_t>
typealias __darwin_fsfilcnt_t = Int

typealias __darwin_gid_tVar = IntVarOf<__darwin_gid_t>
typealias __darwin_gid_t = __uint32_t

typealias __darwin_id_tVar = IntVarOf<__darwin_id_t>
typealias __darwin_id_t = __uint32_t

typealias __darwin_ino64_tVar = LongVarOf<__darwin_ino64_t>
typealias __darwin_ino64_t = __uint64_t

typealias __darwin_ino_tVar = LongVarOf<__darwin_ino_t>
typealias __darwin_ino_t = __darwin_ino64_t

typealias __darwin_mach_port_name_tVar = IntVarOf<__darwin_mach_port_name_t>
typealias __darwin_mach_port_name_t = __darwin_natural_t

typealias __darwin_mach_port_tVar = IntVarOf<__darwin_mach_port_t>
typealias __darwin_mach_port_t = __darwin_mach_port_name_t

typealias __darwin_mode_tVar = ShortVarOf<__darwin_mode_t>
typealias __darwin_mode_t = __uint16_t

typealias __darwin_off_tVar = LongVarOf<__darwin_off_t>
typealias __darwin_off_t = __int64_t

typealias __darwin_pid_tVar = IntVarOf<__darwin_pid_t>
typealias __darwin_pid_t = __int32_t

typealias __darwin_sigset_tVar = IntVarOf<__darwin_sigset_t>
typealias __darwin_sigset_t = __uint32_t

typealias __darwin_suseconds_tVar = IntVarOf<__darwin_suseconds_t>
typealias __darwin_suseconds_t = __int32_t

typealias __darwin_uid_tVar = IntVarOf<__darwin_uid_t>
typealias __darwin_uid_t = __uint32_t

typealias __darwin_useconds_tVar = IntVarOf<__darwin_useconds_t>
typealias __darwin_useconds_t = __uint32_t

typealias __darwin_uuid_tVar = CPointerVarOf<__darwin_uuid_t>
typealias __darwin_uuid_t = CArrayPointer<ByteVar>

typealias __darwin_uuid_string_tVar = CPointerVarOf<__darwin_uuid_string_t>
typealias __darwin_uuid_string_t = CArrayPointer<ByteVar>

typealias __darwin_pthread_attr_t = _opaque_pthread_attr_t

typealias __darwin_pthread_cond_t = _opaque_pthread_cond_t

typealias __darwin_pthread_condattr_t = _opaque_pthread_condattr_t

typealias __darwin_pthread_key_tVar = LongVarOf<__darwin_pthread_key_t>
typealias __darwin_pthread_key_t = Long

typealias __darwin_pthread_mutex_t = _opaque_pthread_mutex_t

typealias __darwin_pthread_mutexattr_t = _opaque_pthread_mutexattr_t

typealias __darwin_pthread_once_t = _opaque_pthread_once_t

typealias __darwin_pthread_rwlock_t = _opaque_pthread_rwlock_t

typealias __darwin_pthread_rwlockattr_t = _opaque_pthread_rwlockattr_t

typealias __darwin_pthread_tVar = CPointerVarOf<__darwin_pthread_t>
typealias __darwin_pthread_t = CPointer<_opaque_pthread_t>

typealias __darwin_nl_itemVar = IntVarOf<__darwin_nl_item>
typealias __darwin_nl_item = Int

typealias __darwin_wctrans_tVar = IntVarOf<__darwin_wctrans_t>
typealias __darwin_wctrans_t = Int

typealias __darwin_wctype_tVar = IntVarOf<__darwin_wctype_t>
typealias __darwin_wctype_t = __uint32_t

typealias clock_tVar = LongVarOf<clock_t>
typealias clock_t = __darwin_clock_t

typealias size_tVar = LongVarOf<size_t>
typealias size_t = __darwin_size_t

typealias time_tVar = LongVarOf<time_t>
typealias time_t = __darwin_time_t

typealias CXVirtualFileOverlayVar = CPointerVarOf<CXVirtualFileOverlay>
typealias CXVirtualFileOverlay = CPointer<CXVirtualFileOverlayImpl>

typealias CXModuleMapDescriptorVar = CPointerVarOf<CXModuleMapDescriptor>
typealias CXModuleMapDescriptor = CPointer<CXModuleMapDescriptorImpl>

typealias CXIndexVar = CPointerVarOf<CXIndex>
typealias CXIndex = COpaquePointer

typealias CXTranslationUnitVar = CPointerVarOf<CXTranslationUnit>
typealias CXTranslationUnit = CPointer<CXTranslationUnitImpl>

typealias CXClientDataVar = CPointerVarOf<CXClientData>
typealias CXClientData = COpaquePointer

typealias CXFileVar = CPointerVarOf<CXFile>
typealias CXFile = COpaquePointer

typealias CXDiagnosticVar = CPointerVarOf<CXDiagnostic>
typealias CXDiagnostic = COpaquePointer

typealias CXDiagnosticSetVar = CPointerVarOf<CXDiagnosticSet>
typealias CXDiagnosticSet = COpaquePointer

typealias CXCursorSetVar = CPointerVarOf<CXCursorSet>
typealias CXCursorSet = CPointer<CXCursorSetImpl>

typealias CXCursorVisitorVar = CPointerVarOf<CXCursorVisitor>
typealias CXCursorVisitor = CPointer<CFunction<(CValue<CXCursor>, CValue<CXCursor>, CXClientData?) -> CXChildVisitResult>>

typealias CXModuleVar = CPointerVarOf<CXModule>
typealias CXModule = COpaquePointer

typealias CXCompletionStringVar = CPointerVarOf<CXCompletionString>
typealias CXCompletionString = COpaquePointer

typealias CXInclusionVisitorVar = CPointerVarOf<CXInclusionVisitor>
typealias CXInclusionVisitor = CPointer<CFunction<(CXFile?, CPointer<CXSourceLocation>?, Int, CXClientData?) -> Unit>>

typealias CXEvalResultVar = CPointerVarOf<CXEvalResult>
typealias CXEvalResult = COpaquePointer

typealias CXRemappingVar = CPointerVarOf<CXRemapping>
typealias CXRemapping = COpaquePointer

typealias CXIdxClientFileVar = CPointerVarOf<CXIdxClientFile>
typealias CXIdxClientFile = COpaquePointer

typealias CXIdxClientEntityVar = CPointerVarOf<CXIdxClientEntity>
typealias CXIdxClientEntity = COpaquePointer

typealias CXIdxClientContainerVar = CPointerVarOf<CXIdxClientContainer>
typealias CXIdxClientContainer = COpaquePointer

typealias CXIdxClientASTFileVar = CPointerVarOf<CXIdxClientASTFile>
typealias CXIdxClientASTFile = COpaquePointer

typealias CXIndexActionVar = CPointerVarOf<CXIndexAction>
typealias CXIndexAction = COpaquePointer

typealias CXFieldVisitorVar = CPointerVarOf<CXFieldVisitor>
typealias CXFieldVisitor = CPointer<CFunction<(CValue<CXCursor>, CXClientData?) -> CXVisitorResult>>

private val loadLibrary = System.loadLibrary("clangstubs")
