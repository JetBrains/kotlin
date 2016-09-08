This library is cleaned up and minfied version
of code represented at
http://vedder.se/2012/07/usb-serial-on-stm32f4/

== IMPORTANT ==
When you plug your STM32 MCU into Linux based computer
you'll find new serial port device such as
/dev/ttyACMx or /dev/ttyACCx.
If you open such a file and write to it in a usual way
you'll send data to the MCU. If you read from it you'll
recieve data from the MCU.
But this device is not a simple serial 2-directional pipe.
Linux wraps this serial into a thing called console or tty.
ttys are usually used for running of the shell on them.
To disable all the shellish tty additions to serial behavior you
need to run:
stty -F /dev/ttyACMx raw -echo -echoe -echok
After this you'll get raw serial port on this device node.

Useful resources:
http://www.armadeus.org/wiki/index.php?title=Serial_ports_usage_on_Linux
http://www.beyondlogic.org/usbnutshell/usb1.shtml
http://www.usbmadesimple.co.uk/index.html
http://www.linusakesson.net/programming/tty/
