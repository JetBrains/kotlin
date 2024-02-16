// !LANGUAGE: -ObjCSignatureOverrideAnnotation
// FIR_IDENTICAL
// WITH_PLATFORM_LIBS

import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*
import platform.CoreFoundation.*
import platform.CoreBluetooth.*

class Delelegate1 : CBCentralManagerDelegateProtocol, NSObject() {
    override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ): Unit = TODO()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral): Unit = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()

    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: NSError?
    ): Unit = TODO()

    override fun centralManagerDidUpdateState(central: CBCentralManager): Unit = TODO()
}

class Delegate2 : CBCentralManagerDelegateProtocol, NSObject() {
    override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ): Unit = TODO()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: NSError?
    ): Unit = TODO()

    override fun centralManagerDidUpdateState(central: CBCentralManager): Unit = TODO()

    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
}

class Delegate3 : CBCentralManagerDelegateProtocol, NSObject() {
    override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ): Unit = TODO()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: NSError?
    ): Unit = TODO()

    override fun centralManagerDidUpdateState(central: CBCentralManager): Unit = TODO()

    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
}

class Delegate4 : CBCentralManagerDelegateProtocol, NSObject() {
    override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ): Unit = TODO()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: NSError?
    ): Unit = TODO()

    override fun centralManagerDidUpdateState(central: CBCentralManager): Unit = TODO()

    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
}

class Delegate5 : CBCentralManagerDelegateProtocol, NSObject() {
    override fun centralManager(central: CBCentralManager, willRestoreState: Map<Any?, *>): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDiscoverPeripheral: CBPeripheral,
        advertisementData: Map<Any?, *>,
        RSSI: NSNumber
    ): Unit = TODO()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral): Unit = TODO()
    override fun centralManager(
        central: CBCentralManager,
        didDisconnectPeripheral: CBPeripheral,
        timestamp: Double,
        isReconnecting: Boolean,
        error: NSError?
    ): Unit = TODO()

    override fun centralManagerDidUpdateState(central: CBCentralManager): Unit = TODO()

    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
    <!CONFLICTING_OVERLOADS!>override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?): Unit<!> = TODO()
}

