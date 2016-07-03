This repository contains source code for Kotlin minicar control facility.


  Raspberry Pi.

 Download Raspberry Pi FS image (for 16G microSD) here:
https://drive.google.com/file/d/0B16LpTmDcUUhLWwyMW5YUDZ5UW8/view?usp=sharing
unbzip2 it, and put it to the card with
sudo dd of=/dev/mmcblk0 if=./microsd.img bs=1M

 Use user 'kotlin', password 'JetBrains', or 'pi', password 'raspberri'.

 If not using provided image, please add file 49-stlinkv2-1.rules
with content 
 SUBSYSTEMS=="usb", ATTRS{idVendor}=="0483", ATTRS{idProduct}=="374b", \
    MODE:="0666", \
    SYMLINK+="stlinkv2-1_%n"
to directory /etc/udev/rules.d.

 To flash a binary program manually, use provided st-flash utility, like this
   st-flash write program.bin 0x08000000

 Also not that st-util can be used as GDB stub for on-device development.
